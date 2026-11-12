/**
 * Module Audit : journal immuable de toutes les transitions et actions sensibles.
 * Les autres modules écrivent par {@link com.cic.audit.application.JournalAuditService}, dans leur
 * propre transaction : l'action et son entrée au journal sont validées ensemble ou pas du tout.
 */
@org.springframework.modulith.ApplicationModule(displayName = "Audit")
package com.cic.audit;
