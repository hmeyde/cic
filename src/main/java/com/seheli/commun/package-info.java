/**
 * Noyau partagé : value objects du domaine utilisés par plusieurs modules (Montant...).
 * Module ouvert : ses types sont du Java pur, sans état ni dépendance vers les autres modules.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Commun",
        type = org.springframework.modulith.ApplicationModule.Type.OPEN)
package com.seheli.commun;
