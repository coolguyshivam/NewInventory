# Data Backup and Archival Strategy for NewInventory

## Overview
This document outlines the recommended strategy for backing up and archiving data from the NewInventory Android application, which uses Firebase Firestore as its primary database.

## Current Data Structure

### Collections
1. **inventory** - Current inventory items with status (AVAILABLE, REPAIR, SOLD, DELETED)
2. **transactions** - All transaction records (Purchase, Sale, Repair, Repair Return, DELETE)

### Data Retention Requirements
- **Active Inventory**: Keep indefinitely while items are in system
- **Transaction History**: Recommended 7 years for audit purposes
- **Deleted Items**: Archive after 90 days, retain for 7 years

## Backup Strategy

### 1. Automated Daily Backups

#### Using Firebase Cloud Functions
```javascript
// Schedule daily backup at 2 AM UTC
exports.scheduledFirestoreBackup = functions.pubsub
  .schedule('0 2 * * *')
  .timeZone('UTC')
  .onRun(async (context) => {
    const projectId = process.env.GCP_PROJECT || process.env.GCLOUD_PROJECT;
    const databaseName = client.databasePath(projectId, '(default)');
    
    const bucket = 'gs://your-backup-bucket/firestore-backups';
    
    await client.exportDocuments({
      name: databaseName,
      outputUriPrefix: `${bucket}/${new Date().toISOString().split('T')[0]}`,
      collectionIds: ['inventory', 'transactions']
    });
    
    console.log('Backup completed successfully');
  });
```

#### Manual Backup via gcloud CLI
```bash
# Export all collections
gcloud firestore export gs://your-backup-bucket/manual-backups/$(date +%Y-%m-%d) \
  --collection-ids=inventory,transactions

# Export specific collection
gcloud firestore export gs://your-backup-bucket/transactions-backup/$(date +%Y-%m-%d) \
  --collection-ids=transactions
```

### 2. Backup Retention Policy

| Backup Type | Retention Period | Storage Location |
|------------|------------------|------------------|
| Daily Backups | 30 days | Firebase Storage |
| Weekly Backups | 3 months | Firebase Storage |
| Monthly Backups | 1 year | Firebase Storage + Cloud Storage Archive |
| Yearly Backups | 7 years | Cloud Storage Archive (Coldline) |

### 3. Backup Verification

Implement automated verification:
```javascript
exports.verifyBackup = functions.storage
  .object()
  .onFinalize(async (object) => {
    if (object.name.includes('firestore-backups')) {
      // Verify backup integrity
      const metadata = await storage.bucket(object.bucket)
        .file(object.name)
        .getMetadata();
      
      // Log verification results
      console.log(`Backup verified: ${object.name}, Size: ${metadata[0].size}`);
      
      // Send notification if verification fails
      if (metadata[0].size < 1000) { // Minimum expected size
        await sendAlertEmail('Backup verification failed');
      }
    }
  });
```

## Archival Strategy

### 1. Data Archival Criteria

**Archive transactions older than 2 years** to a separate archive collection:
- Reduces active database size
- Improves query performance
- Maintains data for compliance

### 2. Archival Process

#### Monthly Archival Job
```javascript
exports.archiveOldTransactions = functions.pubsub
  .schedule('0 3 1 * *') // 1st day of month at 3 AM
  .onRun(async (context) => {
    const db = admin.firestore();
    const twoYearsAgo = Date.now() - (2 * 365 * 24 * 60 * 60 * 1000);
    
    const snapshot = await db.collection('transactions')
      .where('timestamp', '<', twoYearsAgo)
      .limit(500) // Process in batches
      .get();
    
    const batch = db.batch();
    const archiveBatch = db.batch();
    
    snapshot.docs.forEach(doc => {
      // Copy to archive
      const archiveRef = db.collection('transactions_archive').doc(doc.id);
      archiveBatch.set(archiveRef, {
        ...doc.data(),
        archivedAt: Date.now()
      });
      
      // Delete from active collection
      batch.delete(doc.ref);
    });
    
    await archiveBatch.commit();
    await batch.commit();
    
    console.log(`Archived ${snapshot.size} transactions`);
  });
```

### 3. Archive Storage Structure

```
Firestore Collections:
├── transactions (active, last 2 years)
├── transactions_archive (2-7 years)
└── transactions_archive_cold (>7 years, read-only)

Cloud Storage:
├── backups/
│   ├── daily/
│   ├── weekly/
│   └── monthly/
└── archives/
    ├── year=2024/
    ├── year=2023/
    └── year=2022/
```

### 4. Data Restoration Procedure

#### Restore from Backup
```bash
# List available backups
gcloud firestore operations list

# Restore from specific backup
gcloud firestore import gs://your-backup-bucket/backups/2024-01-15 \
  --collection-ids=inventory,transactions
```

#### Query Archived Data
```kotlin
// In app code - query archive when needed
suspend fun getArchivedTransactions(serial: String): List<Transaction> {
    val activeTransactions = db.collection("transactions")
        .whereEqualTo("serial", serial)
        .get()
        .await()
        .toObjects<Transaction>()
    
    val archivedTransactions = db.collection("transactions_archive")
        .whereEqualTo("serial", serial)
        .get()
        .await()
        .toObjects<Transaction>()
    
    return activeTransactions + archivedTransactions
}
```

## Implementation Timeline

### Phase 1: Immediate (Week 1-2)
- [ ] Set up Firebase Storage bucket for backups
- [ ] Implement manual backup script
- [ ] Create backup verification process
- [ ] Document backup/restore procedures

### Phase 2: Automated Backups (Week 3-4)
- [ ] Deploy Cloud Function for automated daily backups
- [ ] Set up retention policy for different backup types
- [ ] Implement backup monitoring and alerts
- [ ] Test restore procedure

### Phase 3: Archival System (Week 5-8)
- [ ] Create archive collections structure
- [ ] Implement archival Cloud Function
- [ ] Update app queries to include archive searches
- [ ] Test end-to-end archival and retrieval

### Phase 4: Monitoring & Optimization (Ongoing)
- [ ] Set up Stackdriver monitoring for backup/archive jobs
- [ ] Create dashboard for backup/archive status
- [ ] Regularly test restore procedures
- [ ] Optimize query performance

## Cost Optimization

### Storage Costs
- **Firestore**: ~$0.18/GB/month
- **Cloud Storage Standard**: ~$0.02/GB/month
- **Cloud Storage Coldline**: ~$0.004/GB/month

### Recommendations
1. Archive data older than 2 years to reduce Firestore costs
2. Move yearly backups to Coldline storage
3. Delete daily backups after 30 days
4. Compress exported data before archiving

### Estimated Monthly Costs (Based on 10GB data)
| Component | Monthly Cost |
|-----------|-------------|
| Active Firestore | $1.80 |
| Archive Firestore (2-7 years) | $7.20 |
| Cloud Storage Backups | $0.60 |
| Coldline Storage (>7 years) | $0.28 |
| **Total** | **~$10/month** |

## Security Considerations

1. **Access Control**: Restrict backup bucket access to service accounts only
2. **Encryption**: Enable encryption at rest for all backups
3. **Audit Logging**: Enable Cloud Audit Logs for backup operations
4. **Versioning**: Enable object versioning in Cloud Storage
5. **Disaster Recovery**: Store yearly backups in multi-region bucket

## Compliance Notes

- Transaction data retention complies with typical audit requirements (7 years)
- Deleted item transactions are retained for audit trail
- Backup verification ensures data integrity
- Access logs provide audit trail for data access

## Monitoring and Alerts

Set up alerts for:
- Backup failures
- Archive job failures
- Backup size anomalies
- Storage quota warnings
- Restore operation requests

## Contact Information

**Primary Contact**: System Administrator
**Backup Storage**: `gs://your-app-backups/`
**Firestore Project**: `your-project-id`
**Documentation**: This file + Firebase Console

---

**Last Updated**: 2024-11-12
**Version**: 1.0
**Author**: Development Team
