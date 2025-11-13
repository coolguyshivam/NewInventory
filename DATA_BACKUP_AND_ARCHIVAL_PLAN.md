# Data Backup and Archival Plan for NewInventory App

## Overview
This document outlines the strategy for backing up and archiving data from the NewInventory Android app which uses Firebase Firestore as its primary database.

## Current Data Structure
The app stores data in Firebase Firestore with the following main collections:
- **inventory**: Current inventory items with status (AVAILABLE, REPAIR, SOLD, DELETED)
- **transactions**: All transaction records (Purchase, Sale, Repair, Repair Return, Edit, Delete)
- **users**: User authentication and role data

## Backup Strategy

### 1. Automated Cloud Backup
**Using Firebase Extensions:**
- Install the "Export Collections to Cloud Storage" Firebase Extension
- Configure automatic daily backups to Google Cloud Storage
- Retention policy: Keep daily backups for 30 days, weekly backups for 6 months, monthly backups for 2 years

**Setup Steps:**
1. Navigate to Firebase Console → Extensions
2. Install "Firestore Backup" extension
3. Configure Cloud Storage bucket: `gs://[project-id]-backups`
4. Set schedule: Daily at 2:00 AM (low-traffic time)
5. Collections to backup: `inventory`, `transactions`, `users`

### 2. Manual Backup Options
**For Critical Operations:**
- Before major data migrations or updates
- Before bulk delete operations
- Monthly manual verification backup

**Commands:**
```bash
# Export Firestore data using gcloud CLI
gcloud firestore export gs://[project-id]-backups/manual-backup-$(date +%Y%m%d)

# Export specific collections
gcloud firestore export gs://[project-id]-backups/manual-backup-$(date +%Y%m%d) \
  --collection-ids=inventory,transactions,users
```

## Archival Strategy

### 3. Data Archival Rules
**When to Archive:**
- Transactions older than 2 years
- Deleted items (status: DELETED) older than 6 months
- Sold items older than 1 year

**Archival Process:**

#### Step 1: Identify Data for Archival
```javascript
// Cloud Function to identify old data
const archiveThreshold = Date.now() - (2 * 365 * 24 * 60 * 60 * 1000); // 2 years

const oldTransactions = await db.collection('transactions')
  .where('timestamp', '<', archiveThreshold)
  .get();
```

#### Step 2: Export to Cold Storage
- Move archived data to Google Cloud Storage (Coldline or Archive class)
- Store in structured format (JSON or Parquet)
- Create index file for quick lookup

#### Step 3: Remove from Active Database
- After successful archival verification
- Keep metadata entry with archive reference
- Maintain audit log of archived records

### 4. Archive Storage Structure
```
gs://[project-id]-archives/
├── 2024/
│   ├── transactions/
│   │   ├── january.json
│   │   ├── february.json
│   │   └── ...
│   └── inventory/
│       ├── deleted-items-q1.json
│       └── ...
├── 2023/
│   └── ...
└── index.json (searchable index)
```

## Implementation Plan

### Phase 1: Basic Backup (Immediate)
- [ ] Enable Firebase automatic backups
- [ ] Configure Cloud Storage bucket
- [ ] Set up daily backup schedule
- [ ] Test restore process
- [ ] Document restore procedures

### Phase 2: Archival System (1-2 months)
- [ ] Create Cloud Function for identifying old data
- [ ] Implement archival process
- [ ] Build archive search functionality
- [ ] Create admin UI for archive access
- [ ] Test end-to-end archival workflow

### Phase 3: Monitoring & Alerts (Ongoing)
- [ ] Set up backup success/failure alerts
- [ ] Monitor storage costs
- [ ] Track database growth rates
- [ ] Regular restore testing (quarterly)

## Data Retention Policies

| Data Type | Active Storage | Archive Storage | Total Retention |
|-----------|---------------|-----------------|-----------------|
| Active Inventory | Unlimited | N/A | Unlimited |
| Recent Transactions | 2 years | 5 years | 7 years |
| Deleted Items | 6 months | 3 years | 3.5 years |
| User Data | Active users | N/A | Unlimited |
| Audit Logs | 1 year | 6 years | 7 years |

## Recovery Procedures

### Disaster Recovery
1. **Full Database Recovery:**
   ```bash
   gcloud firestore import gs://[project-id]-backups/[backup-date]
   ```

2. **Selective Collection Recovery:**
   ```bash
   gcloud firestore import gs://[project-id]-backups/[backup-date] \
     --collection-ids=inventory
   ```

3. **Point-in-Time Recovery:**
   - Use Firebase's built-in point-in-time recovery (35-day window)
   - For older data, restore from Cloud Storage backups

### Archive Data Access
1. **Query Archive Index:**
   - Search index.json for relevant archive file
   
2. **Download Archive File:**
   ```bash
   gsutil cp gs://[project-id]-archives/2023/transactions/march.json ./
   ```

3. **Parse and Display:**
   - Use admin tool to parse JSON and display to user
   - Option to restore specific records to active database

## Cost Optimization

### Storage Tiers
- **Active Data (Firestore):** Standard pricing
- **Recent Backups (Standard Storage):** 30 days
- **Long-term Backups (Nearline):** 6 months to 2 years
- **Archives (Coldline/Archive):** 2+ years

### Estimated Monthly Costs (for moderate usage)
- Firestore: $25-50 (1GB active data)
- Standard Backups: $5-10 (30GB/month)
- Nearline Storage: $10-15 (100GB)
- Coldline/Archive: $5-8 (200GB)
- **Total: ~$45-83/month**

## Security & Compliance

### Data Protection
- All backups encrypted at rest (Google-managed keys)
- Access restricted via IAM roles
- Audit logging enabled for all backup/restore operations

### Compliance Considerations
- GDPR: Right to erasure - ensure deleted user data is permanently removed from backups after retention period
- Data sovereignty: Store backups in appropriate regional Cloud Storage buckets
- Audit trail: Maintain logs of all backup, archival, and restore operations

## Monitoring & Alerts

### Key Metrics to Track
- Backup success rate (target: 99.9%)
- Backup duration
- Storage growth rate
- Archive operations completed
- Failed restore attempts

### Alert Configuration
- Email alert on backup failure
- Weekly backup status report
- Monthly storage cost report
- Quarterly restore test reminder

## Testing Schedule

| Test Type | Frequency | Responsibility |
|-----------|-----------|---------------|
| Backup verification | Weekly | Automated |
| Partial restore test | Monthly | Admin |
| Full restore test | Quarterly | Development team |
| Disaster recovery drill | Annually | Full team |

## Documentation & Training

### Required Documentation
- [ ] Backup restoration step-by-step guide
- [ ] Archive search and retrieval guide
- [ ] Emergency contact list
- [ ] Disaster recovery runbook

### Training Requirements
- Admin users: Backup verification and manual backup procedures
- Development team: Full restore and disaster recovery procedures
- Support team: Archive data retrieval procedures

## Review and Updates
This plan should be reviewed and updated:
- Quarterly: Review metrics and adjust retention policies
- Annually: Full plan review and disaster recovery drill
- As needed: After any significant system changes

---

**Plan Version:** 1.0  
**Last Updated:** 2024-01-XX  
**Next Review Date:** 2024-04-XX  
**Plan Owner:** Development Team Lead
