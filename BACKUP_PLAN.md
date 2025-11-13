# Firestore Backup and Archival Plan

## Overview
This document describes the backup and archival strategy for the NewInventory application's Firestore database to ensure data integrity, compliance, and cost optimization.

## Backup Strategy

### 1. Nightly Transaction Archival

**Purpose:** Move old transaction data to an archive collection to optimize query performance and reduce storage costs.

**Schedule:** Daily at 2:00 AM UTC

**Process:**
- A Cloud Function (`archiveOldTransactions`) runs nightly
- Identifies all transactions older than 180 days
- Moves qualifying transactions from `transactions` collection to `transactions_archive` collection
- Maintains all original transaction data including:
  - Transaction type, model, serial
  - Customer information (name, phone, aadhaar)
  - Amount, quantity, description
  - Date, timestamp, userRole
  - Images and deletion info

**Implementation:**
```javascript
// Cloud Function: archiveOldTransactions
const functions = require('firebase-functions');
const admin = require('firebase-admin');
admin.initializeApp();

const db = admin.firestore();
const DAYS_TO_ARCHIVE = 180;

exports.archiveOldTransactions = functions.pubsub
  .schedule('0 2 * * *')  // Daily at 2:00 AM UTC
  .timeZone('UTC')
  .onRun(async (context) => {
    const cutoffDate = new Date();
    cutoffDate.setDate(cutoffDate.getDate() - DAYS_TO_ARCHIVE);
    const cutoffTimestamp = cutoffDate.getTime();

    const snapshot = await db.collection('transactions')
      .where('timestamp', '<', cutoffTimestamp)
      .limit(500)  // Process in batches
      .get();

    if (snapshot.empty) {
      console.log('No transactions to archive');
      return null;
    }

    const batch = db.batch();
    let count = 0;

    snapshot.forEach((doc) => {
      const archiveRef = db.collection('transactions_archive').doc(doc.id);
      batch.set(archiveRef, doc.data());
      
      const transactionRef = db.collection('transactions').doc(doc.id);
      batch.delete(transactionRef);
      
      count++;
    });

    await batch.commit();
    console.log(`Archived ${count} transactions older than ${DAYS_TO_ARCHIVE} days`);
    
    return null;
  });
```

### 2. Weekly Firestore Export to Google Cloud Storage

**Purpose:** Create a complete backup of all Firestore data for disaster recovery and compliance.

**Schedule:** Weekly on Sundays at 3:00 AM UTC

**Storage Location:** `gs://newinventory-backups/firestore-exports/`

**Retention Policy:** Keep weekly backups for 90 days, then delete

**Implementation:**

#### Option A: Using Cloud Scheduler + Cloud Functions
```javascript
// Cloud Function: exportFirestore
const functions = require('firebase-functions');
const firestore = require('@google-cloud/firestore');

const client = new firestore.v1.FirestoreAdminClient();

exports.exportFirestore = functions.pubsub
  .schedule('0 3 * * 0')  // Every Sunday at 3:00 AM UTC
  .timeZone('UTC')
  .onRun(async (context) => {
    const projectId = process.env.GCP_PROJECT || process.env.GCLOUD_PROJECT;
    const databaseName = client.databasePath(projectId, '(default)');
    
    const timestamp = new Date().toISOString().split('T')[0];
    const outputUriPrefix = `gs://newinventory-backups/firestore-exports/${timestamp}`;

    try {
      const [operation] = await client.exportDocuments({
        name: databaseName,
        outputUriPrefix: outputUriPrefix,
        collectionIds: ['inventory', 'transactions', 'transactions_archive']
      });

      console.log(`Export operation started: ${operation.name}`);
      console.log(`Exporting to: ${outputUriPrefix}`);
      
      return { success: true, operation: operation.name };
    } catch (error) {
      console.error('Export failed:', error);
      throw error;
    }
  });
```

#### Option B: Using gcloud command (Manual or CI/CD)
```bash
#!/bin/bash
# backup-firestore.sh

DATE=$(date +%Y-%m-%d)
BUCKET="gs://newinventory-backups/firestore-exports"
PROJECT_ID="your-project-id"

gcloud firestore export \
  --project="$PROJECT_ID" \
  --collection-ids='inventory,transactions,transactions_archive' \
  "$BUCKET/$DATE"

# Clean up backups older than 90 days
CUTOFF_DATE=$(date -d '90 days ago' +%Y-%m-%d)
gsutil -m rm -r "$BUCKET/*" 2>/dev/null | grep -v "$CUTOFF_DATE" || true
```

### 3. Backup Storage Configuration

**Google Cloud Storage Bucket Setup:**
```bash
# Create backup bucket
gsutil mb -p newinventory -c STANDARD -l us-central1 gs://newinventory-backups

# Set lifecycle policy for automatic deletion after 90 days
cat > lifecycle.json << EOF
{
  "lifecycle": {
    "rule": [
      {
        "action": {"type": "Delete"},
        "condition": {"age": 90}
      }
    ]
  }
}
EOF

gsutil lifecycle set lifecycle.json gs://newinventory-backups

# Set bucket permissions (restrict to service account)
gsutil iam ch serviceAccount:firebase-adminsdk@newinventory.iam.gserviceaccount.com:objectAdmin gs://newinventory-backups
```

## Data Recovery Procedures

### Recovering from Archive
To retrieve archived transactions:
```javascript
// Query archived transactions
const archivedSnapshot = await db.collection('transactions_archive')
  .where('serial', '==', serialNumber)
  .orderBy('timestamp', 'desc')
  .get();

archivedSnapshot.forEach((doc) => {
  console.log('Archived transaction:', doc.data());
});
```

### Restoring from Firestore Export
```bash
# Restore entire database
gcloud firestore import \
  --project="your-project-id" \
  gs://newinventory-backups/firestore-exports/2024-01-15

# Restore specific collections
gcloud firestore import \
  --project="your-project-id" \
  --collection-ids='transactions' \
  gs://newinventory-backups/firestore-exports/2024-01-15
```

## Monitoring and Alerts

### Cloud Function Monitoring
- Set up Cloud Monitoring alerts for function failures
- Monitor function execution time and success rate
- Alert on backup failures via email/SMS

### Storage Monitoring
- Monitor GCS bucket size and growth rate
- Alert if bucket size exceeds expected thresholds
- Track backup completion and data integrity

## Security Considerations

1. **Access Control:**
   - Limit Firestore admin access to specific service accounts
   - Use IAM roles to restrict backup bucket access
   - Enable audit logging for all backup operations

2. **Encryption:**
   - All data is encrypted at rest in Firestore
   - GCS backups are encrypted using Google-managed keys
   - Consider customer-managed encryption keys (CMEK) for sensitive data

3. **Compliance:**
   - Maintain backups for minimum 90 days for audit purposes
   - Archive transactions for historical record keeping
   - Document all data retention policies

## Cost Optimization

**Estimated Costs:**
- Firestore storage: ~$0.18/GB/month
- GCS Standard storage: ~$0.02/GB/month
- Cloud Functions: ~$0.40/million invocations
- Network egress: ~$0.12/GB

**Optimization Strategies:**
1. Archive old transactions to reduce active Firestore storage
2. Use lifecycle policies to auto-delete old backups
3. Compress exports when storing long-term
4. Use Nearline or Coldline storage for backups older than 30 days

## Testing and Validation

**Monthly Backup Testing:**
1. Randomly select a backup from the previous month
2. Restore to a test Firestore instance
3. Verify data integrity and completeness
4. Document test results and any issues found

**Quarterly Disaster Recovery Drill:**
1. Simulate complete data loss scenario
2. Restore from most recent backup
3. Validate all application functionality
4. Measure recovery time objective (RTO) and recovery point objective (RPO)

## Maintenance Schedule

- **Daily:** Run archive function
- **Weekly:** Create Firestore export backup
- **Monthly:** Test backup restoration
- **Quarterly:** Full disaster recovery drill
- **Annually:** Review and update backup strategy

## Contact Information

For backup-related issues:
- **Primary:** DevOps Team (devops@example.com)
- **Secondary:** Database Administrator (dba@example.com)
- **Emergency:** On-call Engineer (oncall@example.com)

## Revision History

| Date | Version | Changes | Author |
|------|---------|---------|--------|
| 2024-01-15 | 1.0 | Initial backup plan | GitHub Copilot |
