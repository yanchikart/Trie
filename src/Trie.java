import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;
public final class Trie{
    private static final class Node{
        char[] keys = new char[0];
        Node[] vals = new Node[0];
        boolean terminal = false;
        int size = 0;
        int indexOf(char key) {
            int lo = 0, hi = size - 1;
            while (lo <= hi) {
                int mid = (lo + hi) >>> 1;
                char k = keys[mid];
                if (k < key) lo = mid + 1;
                else if (k > key) hi = mid - 1;
                else return mid;
            }
            return -1;
        }

        Node get(char key){
            int in =  indexOf(key);

            if(in == -1 ) return null;
            else return vals[in];
        }
        int getIndex(char key) {
            return indexOf(key);
        }

        Node getOrCreateWithQuickSort(char key) {
            int in = indexOf(key);
            if (in >= 0) return vals[in];

            ensureSize(size + 1);
            keys[size] = key;
            Node node = new Node();
            vals[size] = node;
            size++;

            int last = size - 1;
            if (last == 0 || keys[last - 1] <= keys[last]) {
                return node; // уже отсортировано — выходим
            }

            // Сортируем весь рабочий диапазон [0..size-1]
            quickSort(keys, vals, 0, size - 1);
            return node;
        }

        void ensureSize(int size_min){
            if (keys.length >= size_min) return;
            int newSize = (keys.length == 0) ? 4 : (keys.length + (keys.length >>> 1));
            if (size_min > newSize) newSize = size_min;
            char[] nk = new char[newSize];
            Node[] nv = new Node[newSize];
            for(int i = 0; i < size; i++){
                nk[i] = keys[i];
                nv[i] = vals[i];
            }
            keys = nk;
            vals = nv;

        }

        void removeChildAt(int idx) {

            for (int i = idx; i < size - 1; i++) {
                keys[i] = keys[i + 1];
                vals[i] = vals[i + 1];
            }
            if (size > 0) {
                vals[size - 1] = null;
            }
            size--;
        }
        private static void quickSort(char[] a, Node[] b, int lo, int hi) {
            while (lo < hi) {
                int i = lo, j = hi;
                char pivot = a[(lo + hi) >>> 1];

                // разбиение
                while (i <= j) {
                    while (a[i] < pivot) i++;
                    while (a[j] > pivot) j--;
                    if (i <= j) {
                        char tk = a[i]; a[i] = a[j]; a[j] = tk;
                        Node tv = b[i]; b[i] = b[j]; b[j] = tv;
                        i++; j--;
                    }
                }
                if (j - lo < hi - i) {
                    if (lo < j) quickSort(a, b, lo, j);
                    lo = i;
                } else {
                    if (i < hi) quickSort(a, b, i, hi);
                    hi = j;
                }
            }
        }
    }

    private static final class StringAcc{
        String[] str1 = new String[8];
        int n = 0;

        void add(String str){
            if (n == str1.length){
                int nc = str1.length + (str1.length >>> 1);
                if(str1.length + 1> nc) nc = str1.length+ 1;
                str1 = java.util.Arrays.copyOf(str1, nc);

            }
            str1[n++] = str;
        }


        String[] toArray() {
            return java.util.Arrays.copyOf(str1, n);
        }
    }
    private final Node root = new Node();
    private int words = 0;
    public void insert(String word){
        if (word == null) throw new IllegalArgumentException("word == null");
        Node curr = root;
        for (int i = 0; i < word.length(); i++) {
            curr = curr.getOrCreateWithQuickSort(word.charAt(i));
        }
        if (!curr.terminal) {
            curr.terminal = true;
            words++;
        }
    }
    public boolean contains(String word){
        if (word == null) return false;
        Node node = nodeFor(word);
        if (node == null) return false;
        return node.terminal;
    }
    public boolean startsWith(String prefix){
        if (prefix == null) return false;
        return nodeFor(prefix) != null;
    }
    public String[] getByPrefix(String prefix){
        if (prefix == null) throw new IllegalArgumentException("prefix == null");
        Node start = nodeFor(prefix);
        if (start == null) return new String[0];
        StringAcc acc = new StringAcc();
        StringBuilder sb = new StringBuilder(prefix);
        collect(start, sb, acc);
        return acc.toArray();
    }

    public int size() { return words; }

    public String getLongestWord() {
        if (words == 0) return null;
        Best best = new Best();
        StringBuilder sb = new StringBuilder();
        dfsLongest(root, sb, best);
        return best.word;
    }

    public boolean remove(String word) {
        if (word == null) return false;
        if (!contains(word)) return false;// чтобы вернуть корректный результат
        removeRec(root, word, 0);// чистим ветки
        words--;
        return true;
    }

    public void clear() {
        clearNode(root);
        words = 0;
    }

    public void printTree() {
        printNode(root, "", true);
    }

     
    public String[] autocomplete(String prefix, int maxSuggestions) {
        if (prefix == null || maxSuggestions <= 0) return new String[0];
        Node start = nodeFor(prefix);
        if (start == null) return new String[0];

        StringAcc acc = new StringAcc();
        StringBuilder sb = new StringBuilder(prefix);
        collectLimited(start, sb, acc, maxSuggestions);
        return acc.toArray();
    }

    private void collectLimited(Node node, StringBuilder sb, StringAcc acc, int limit) {
        if (acc.n >= limit) return;
        if (node.terminal) acc.add(sb.toString());
        for (int i = 0; i < node.size && acc.n < limit; i++) {
            int len = sb.length();
            sb.append(node.keys[i]);
            collectLimited(node.vals[i], sb, acc, limit);
            sb.setLength(len);
        }
    }
    public int loadFromFile(String filename) {
        int added = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                String w = trimLine(line);
                if (w != null) {
                    int before = words;
                    insert(w);
                    if (words > before) added++;
                }
            }
        } catch (IOException e) {
            System.err.println("loadFromFile error: " + e.getMessage());
        }
        return added;
    }

    public int saveToFile(String filename) {
        String[] all = getByPrefix("");
        int saved = 0;
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(filename))) {
            for (String line : all) {
                bw.write(line);
                bw.newLine();
                saved++;
            }
        } catch (IOException e) {
            System.err.println("saveToFile error: " + e.getMessage());
        }
        return saved;
    }

    private Node nodeFor(String str){

        Node curr = root;
        for (int i = 0; i < str.length(); i++) {
            Node next = curr.get(str.charAt(i));
            if (next == null) return null;        // если нет узла - выходим
            curr = next;
        }
        return curr;

    }

    private static String trimLine(String line) {
        if (line == null) return null;
        int start = 0, end = line.length();
        while (start < end && Character.isWhitespace(line.charAt(start))) start++;
        while (end > start && Character.isWhitespace(line.charAt(end - 1))) end--;
        if (start >= end) return null;
        return line.substring(start, end);
    }
    private void collect(Node node, StringBuilder sb, StringAcc acc) {
        if (node.terminal) acc.add(sb.toString());
        for (int i = 0; i < node.size; i++) {
            int len = sb.length();
            sb.append(node.keys[i]);
            collect(node.vals[i], sb, acc);
            sb.setLength(len);
        }
    }
    private boolean removeRec(Node node, String word, int pos) {
        if (pos == word.length()) {
            if (!node.terminal) return false;
            node.terminal = false;

            return true;
        }
        char ch = word.charAt(pos);
        int idx = node.getIndex(ch);
        if (idx == -1) return false;
        Node child = node.vals[idx];
        boolean pruneChild = removeRec(child, word, pos + 1);


        if (pruneChild && !child.terminal && child.size == 0) {
            node.removeChildAt(idx);
        }


        return !node.terminal && node.size == 0 && node != root;
    }
    private void clearNode(Node n) {
        for (int i = 0; i < n.size; i++) {
            if (n.vals[i] != null) clearNode(n.vals[i]);
            n.vals[i] = null;
        }
        n.size = 0;
        n.terminal = false;
        n.keys = new char[0];
        n.vals = new Node[0];
    }
    private static final class Best {
        String word = null;
        int len = -1;
    }
    private void dfsLongest(Node n, StringBuilder sb, Best best) {
        if (n.terminal) {
            int L = sb.length();
            if (L > best.len || (L == best.len && (best.word == null || sb.toString().compareTo(best.word) < 0))) {
                best.len = L;
                best.word = sb.toString();
            }
        }
        for (int i = 0; i < n.size; i++) {
            int len = sb.length();
            sb.append(n.keys[i]);
            dfsLongest(n.vals[i], sb, best);
            sb.setLength(len);
        }
    }
    private void printNode(Node n, String prefix, boolean isLast) {
        // печать терминала (пустой префикс — корень)
        if (n != root && n.terminal) {
            System.out.println(prefix + (isLast ? "└─" : "├─") + "(word)");
        }
        // печать детей с линиями
        for (int i = 0; i < n.size; i++) {
            boolean lastChild = (i == n.size - 1);
            String edge = prefix + (isLast ? "  " : "│ ");
            System.out.println(edge + (lastChild ? "└─" : "├─") + "[" + n.keys[i] + "]");
            printNode(n.vals[i], edge + (lastChild ? "  " : "│ "), lastChild);
        }
    }
    @Override
    public String toString() {
        String[] res = getByPrefix("");
        StringBuilder sb = new StringBuilder("Trie[");
        for (int i = 0; i < res.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append('"').append(res[i]).append('"');
        }
        return sb.append(']').toString();
    }
}
final class TrieDemo {
    private static void showMenu() {
        System.out.println("Trie меню");
        System.out.println(" 1) add               — добавить слово");
        System.out.println(" 2) contains          — проверить слово");
        System.out.println(" 3) startsWith        — есть ли слова с префиксом");
        System.out.println(" 4) prefix            — вывести слова по префиксу");
        System.out.println(" 5) autocomplete      — автодополнение (prefix, n)");
        System.out.println(" 6) remove            — удалить слово");
        System.out.println(" 7) longest           — самое длинное слово");
        System.out.println(" 8) size              — количество слов");
        System.out.println(" 9) tree              — печать дерева");
        System.out.println("10) load              — загрузить из файла");
        System.out.println("11) save              — сохранить в файл");
        System.out.println("12) clear             — очистить словарь");
        System.out.println(" 0) exit              — выход");
    }

    private static int readInt(Scanner sc, String prompt) {
        while (true) {
            System.out.print(prompt);
            String s = sc.nextLine().trim();
            try { return Integer.parseInt(s); }
            catch (NumberFormatException e) { System.out.println("Введите число."); }
        }
    }

    private static String readStr(Scanner sc, String prompt) {
        System.out.print(prompt);
        return sc.nextLine().trim();
    }

    public static void main(String[] args) {
        Trie t = new Trie();
        Scanner sc = new Scanner(System.in);

        while (true) {
            showMenu();
            int choice = readInt(sc, "> ");

            switch (choice) {
                case 1: { // add
                    String w = readStr(sc, "слово: ");
                    if (!w.isEmpty()) { t.insert(w); System.out.println("OK"); }
                    else System.out.println("Пустое слово не добавляется.");
                    break;
                }
                case 2: { // contains
                    String w = readStr(sc, "слово: ");
                    System.out.println(t.contains(w));
                    break;
                }
                case 3: { // startsWith
                    String p = readStr(sc, "префикс: ");
                    System.out.println(t.startsWith(p));
                    break;
                }
                case 4: { // prefix -> список слов
                    String p = readStr(sc, "префикс: ");
                    if (!t.startsWith(p)) {
                        System.out.println("(нет слов по префиксу)");
                    } else {
                        String[] arr = t.getByPrefix(p);
                        for (String element : arr)
                            System.out.println(element);
                    }
                    break;
                }
                case 5: { // autocomplete
                    String p = readStr(sc, "префикс: ");
                    int n = readInt(sc, "сколько подсказок (n>0): ");
                    if (n <= 0) { System.out.println("n должно быть > 0"); break; }
                    if (!t.startsWith(p)) {
                        System.out.println("(нет слов по префиксу)");
                    } else {
                        String[] a = t.autocomplete(p, n);
                        for (String element : a)
                            System.out.println(element);
                    }
                    break;
                }
                case 6: { // remove
                    String w = readStr(sc, "слово: ");
                    System.out.println(t.remove(w) ? "removed" : "not found");
                    break;
                }
                case 7: { // longest
                    String s = t.getLongestWord();
                    System.out.println(s == null ? "(empty)" : s);
                    break;
                }
                case 8: { // size
                    System.out.println(t.size());
                    break;
                }
                case 9: { // tree
                    t.printTree();
                    break;
                }
                case 10: { // load
                    String f = readStr(sc, "файл: ");
                    System.out.println("Загружено: " + t.loadFromFile(f));
                    break;
                }
                case 11: { // save
                    String f = readStr(sc, "файл: ");
                    System.out.println("Сохранено: " + t.saveToFile(f));
                    break;
                }
                case 12: { // clear
                    t.clear();
                    System.out.println("OK");
                    break;
                }
                case 0:
                    System.out.println("Bye!");
                    sc.close();
                    return;
                default:
                    System.out.println("Неверный пункт. Введите 0..12.");
            }
            System.out.println();
        }
    }
}