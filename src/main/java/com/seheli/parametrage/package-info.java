/**
 * Module Paramétrage : règles de gestion stockées dans la table {@code parametre},
 * lues au démarrage et rechargées périodiquement, modifiables sans redéploiement.
 *
 * <p>API publique : {@code domain} (clés et valeurs typées) et {@code application}
 * ({@link com.seheli.parametrage.application.ParametreService}). La persistance reste interne.
 */
@org.springframework.modulith.ApplicationModule(displayName = "Paramétrage")
package com.seheli.parametrage;
