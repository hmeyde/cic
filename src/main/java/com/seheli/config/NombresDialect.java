package com.seheli.config;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.thymeleaf.context.IExpressionContext;
import org.thymeleaf.dialect.AbstractDialect;
import org.thymeleaf.dialect.IExpressionObjectDialect;
import org.thymeleaf.expression.IExpressionObjectFactory;

/**
 * Objet d'expression {@code #nombres} des gabarits : montants et pourcentages lisibles en français et en arabe.
 * <p>
 * Les milliers sont séparés par une espace insécable (U+00A0) et non par une espace ordinaire : dans une page
 * {@code dir="rtl"}, l'algorithme bidirectionnel Unicode traite l'espace ordinaire entre deux groupes de chiffres
 * comme un séparateur de mots et inverse leur ordre (« 20 800 » s'affichait « 800 20 »). L'espace insécable,
 * séparateur de nombre (classe CS), garde le nombre d'un seul tenant et empêche aussi la coupure à la ligne.
 * Chiffres latins dans les deux langues, virgule décimale.
 */
@Component
public class NombresDialect extends AbstractDialect implements IExpressionObjectDialect {

    static final char SEPARATEUR_MILLIERS = ' ';
    private static final String NOM = "nombres";

    public NombresDialect() {
        super("Nombres Seheli");
    }

    @Override
    public IExpressionObjectFactory getExpressionObjectFactory() {
        return new IExpressionObjectFactory() {
            @Override
            public Set<String> getAllExpressionObjectNames() {
                return Set.of(NOM);
            }

            @Override
            public Object buildObject(IExpressionContext context, String expressionObjectName) {
                return NOM.equals(expressionObjectName) ? new Nombres() : null;
            }

            @Override
            public boolean isCacheable(String expressionObjectName) {
                return true;
            }
        };
    }

    /** Méthodes appelées depuis les gabarits : {@code #nombres.entier(x)}, {@code #nombres.decimal(x, 1)}. */
    public static final class Nombres {

        /** Entier arrondi à l'unité (HALF_UP), milliers séparés par une espace insécable : « 20 800 ». */
        public String entier(Number valeur) {
            return valeur == null ? "" : format(0).format(versDecimal(valeur).setScale(0, RoundingMode.HALF_UP));
        }

        /** Nombre à {@code decimales} chiffres au plus après la virgule : « 12,5 », « 1 234,5 ». */
        public String decimal(Number valeur, int decimales) {
            return valeur == null ? "" : format(decimales).format(versDecimal(valeur).setScale(decimales, RoundingMode.HALF_UP));
        }

        private static BigDecimal versDecimal(Number valeur) {
            return valeur instanceof BigDecimal b ? b : new BigDecimal(valeur.toString());
        }

        private static DecimalFormat format(int decimales) {
            DecimalFormatSymbols symboles = DecimalFormatSymbols.getInstance(Locale.ROOT);
            symboles.setGroupingSeparator(SEPARATEUR_MILLIERS);
            symboles.setDecimalSeparator(',');
            DecimalFormat format = new DecimalFormat("#,##0", symboles);
            format.setMinimumFractionDigits(decimales);
            format.setMaximumFractionDigits(decimales);
            format.setRoundingMode(RoundingMode.HALF_UP);
            return format;
        }
    }
}
