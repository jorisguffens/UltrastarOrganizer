package be.jorisg.ultrastarorganizer.search;

import java.text.Normalizer;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SearchEngine<T> {

    private final static Pattern[] BRACKET_PATTERNS = new Pattern[]{
            Pattern.compile("[(]([^)]*)[)]"),
            Pattern.compile("\\[([^\\]]*)\\]"),
    };

    private final Set<Index<T>> indexes = new HashSet<>();

    public void index(T option, String key) {
        key = normalize(key);
        indexes.add(new Index<T>(option, key, parse(key)));
    }

    public void remove(T option) {
        indexes.removeIf(i -> i.option.equals(option));
    }

    public SearchResult<T> searchOne(String input) {
        List<SearchResult<T>> result = search(input, 1);
        return result.isEmpty() ? null : result.get(0);
    }

    public List<SearchResult<T>> search(String input) {
        return search(input, Integer.MAX_VALUE);
    }

    public List<SearchResult<T>> search(String input, int limit) {
        input = normalize(input);
        input = input.replaceAll("[^A-Za-z0-9 ]+", "");
        input = input.replace("ft.", "");
        input = input.replace("feat.", "");
        for ( Pattern pattern : BRACKET_PATTERNS) {
            input = pattern.matcher(input).replaceAll("");
        }
        input = input.replaceAll("  ", " ").trim();
        String[] words = input.split(" ");

        PriorityQueue<SearchResult<T>> results = new PriorityQueue<>(Comparator.reverseOrder());

        for (Index<T> index : indexes) {
            SearchResult<T> result = index.match(words);
            if (result.score > 0) results.add(result);
        }

        List<SearchResult<T>> result = new ArrayList<>();
        for (int i = 0; i < limit && i < results.size(); i++) {
            result.add(results.poll());
        }
        return result;
    }

    private String normalize(String str) {
        str = Normalizer.normalize(str, Normalizer.Form.NFD);
        str = str.replaceAll("  ", " ").trim();
        str = str.toLowerCase();
        return str;
    }

    private record Index<T>(T option, String key, KeyBlock[] blocks) {

        private SearchResult<T> match(String[] words) {
            double weight = Arrays.stream(blocks).reduce(0d, (a, b) -> a + b.weight, Double::sum);
            double score = 0;
            Set<String> missing = new HashSet<>(Arrays.asList(words));

            for (KeyBlock block : blocks) {
                Set<String> match = Arrays.stream(words)
                        .filter(s -> List.of(block.values).contains(s))
                        .collect(Collectors.toSet());
                missing.removeAll(match);
                double w = block.weight / weight;
                double bs = (match.size() / (double) block.values.length) * w;
                score += bs;
            }

            double penalty = Math.pow(missing.size() / (double) words.length, 2);
            score *= 1 - penalty;

            return new SearchResult<T>(option, score);
        }

    }

    public record SearchResult<T>(T option, double score) implements Comparable<SearchResult<T>> {
        @Override
        public int compareTo(SearchResult o) {
            return Double.compare(score, o.score);
        }
    }

    private record KeyBlock(String[] values, double weight) {
    }

    private KeyBlock[] parse(String key) {
        return parse(key, 1);
    }

    private KeyBlock[] parse(String key, double weight) {
        List<KeyBlock> blocks = new ArrayList<>();

        for ( Pattern pattern : BRACKET_PATTERNS) {
            Matcher m = pattern.matcher(key); // between brackets
            while (m.find()) {
                String group = m.group(1);
                blocks.addAll(List.of(parse(group, weight / 4)));
                key = key.replace(group, "");
            }
        }

        String[] bls = key.split(", | [\\-&] |ft\\.|feat\\.");
        for (String b : bls) {
            b = b.replaceAll("[^A-Za-z0-9 ]+", "");
            b = b.replaceAll("  ", " ").trim();
            blocks.add(new KeyBlock(b.split(" "), weight));
        }
        return blocks.toArray(KeyBlock[]::new);
    }

}
