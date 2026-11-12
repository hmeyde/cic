import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.*;

/** Liste les routes web des contrôleurs Spring : java Routes.java src/main/java/com/cic */
public class Routes {
    public static void main(String[] args) throws Exception {
        Path base = Path.of(args[0]);
        Pattern ctrl = Pattern.compile("@(Rest)?Controller\\b");
        Pattern cst = Pattern.compile("static final String (\\w+)\\s*=\\s*\"([^\"]*)\"");
        Pattern prefixP = Pattern.compile("(?m)^@RequestMapping\\(\\s*(?:value\\s*=\\s*|path\\s*=\\s*)?\"([^\"]*)\"");
        Pattern map = Pattern.compile("@(Get|Post|Put|Delete|Patch)Mapping(?:\\(([^)]*)\\))?");
        Pattern tok = Pattern.compile("\"[^\"]*\"(?:\\s*\\+\\s*[A-Z_][A-Z0-9_]*)?|[A-Z_][A-Z0-9_]*(?:\\s*\\+\\s*\"[^\"]*\")?");
        List<String> lignes = new ArrayList<>();
        try (Stream<Path> s = Files.walk(base)) {
            for (Path f : s.filter(p -> p.toString().endsWith(".java")).toList()) {
                String t = Files.readString(f);
                if (!ctrl.matcher(t).find()) continue;
                Map<String, String> c = new HashMap<>();
                Matcher m = cst.matcher(t);
                while (m.find()) c.put(m.group(1), m.group(2));
                Matcher comp = Pattern.compile("static final String (\\w+)\\s*=\\s*([A-Z_][A-Z0-9_]*)\\s*\\+\\s*\"([^\"]*)\"").matcher(t);
                while (comp.find()) c.put(comp.group(1), c.getOrDefault(comp.group(2), comp.group(2)) + comp.group(3));
                Matcher pm = prefixP.matcher(t);
                String prefix = pm.find() ? pm.group(1) : "";
                List<String> routes = new ArrayList<>();
                Matcher mm = map.matcher(t);
                while (mm.find()) {
                    String arg = mm.group(2) == null ? "" : mm.group(2);
                    String chemin = "";
                    Matcher tm = tok.matcher(arg);
                    if (tm.find()) {
                        StringBuilder sb = new StringBuilder();
                        for (String part : tm.group().split("\\+")) {
                            part = part.trim();
                            sb.append(part.startsWith("\"") ? part.replace("\"", "") : c.getOrDefault(part, part));
                        }
                        chemin = sb.toString();
                    }
                    String r = prefix + chemin;
                    routes.add("`" + mm.group(1).toUpperCase() + " " + (r.isEmpty() ? "/" : r) + "`");
                }
                String module = base.relativize(f).getName(0).toString();
                String nom = f.getFileName().toString().replace(".java", "");
                lignes.add("| `" + nom + "` (" + module + ") | " + String.join(", ", routes) + " |");
            }
        }
        Collections.sort(lignes);
        lignes.forEach(System.out::println);
    }
}
