package be.jorisg.ultrastarorganizer.search;

import be.jorisg.ultrastarorganizer.UltrastarOrganizer;

import java.text.Normalizer;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;

public class SearchEngine<T> {

    private final Set<Index<T>> indexes = new HashSet<>();

    public final BiConsumer<T, String> indexer = (option, key) -> {
        key = Normalizer.normalize(key, Normalizer.Form.NFD);
        String[] parts = Arrays.stream(key.split(Pattern.quote(" ")))
                .filter(s -> s.length() > 1)
                .map(s -> s.replaceAll("[^A-Za-z0-9]+", ""))
                .toArray(String[]::new);
        indexes.add(new Index<T>(option, key, parts));
    };

    public void removeIndex(T option) {
        indexes.removeIf(i -> i.option.equals(option));
    }

    public SearchResult<T> search(String input) {
        List<SearchResult<T>> result = search(input,  1);
        return result.isEmpty() ? null : result.get(0);
    }

    public List<SearchResult<T>> search(String input, int limit) {
        String ni = Normalizer.normalize(input, Normalizer.Form.NFD);
        String[] inputs = Arrays.stream(ni.split(Pattern.quote(" ")))
                .filter(s -> s.length() > 1)
                .map(s -> s.replaceAll("[^A-Za-z0-9]+", ""))
                .toArray(String[]::new);

        PriorityQueue<SearchResult<T>> results = new PriorityQueue<>(Comparator.reverseOrder());

        for ( Index<T> index : indexes ) {
            results.add(index.match(inputs));
        }

        List<SearchResult<T>> result = new ArrayList<>();
        for ( int i = 0; i < limit && i < results.size(); i++ ) {
            result.add(results.poll());
        }
        return result;
    }

    private record Index<T>(T option, String key, String[] parts) {

        private SearchResult<T> match(String[] inputs) {
            double match = Arrays.stream(inputs).filter(s -> Arrays.stream(parts).anyMatch(k -> k.equalsIgnoreCase(s)))
                    .count();
            double pct = match / parts.length;

            return new SearchResult<T>(option, match, pct, match + pct);
        }

    }

    public record SearchResult<T>(T option, double match, double pctMatch, double score) implements Comparable<SearchResult<T>> {
        @Override
        public int compareTo(SearchResult o) {
            return Double.compare(score, o.score);
        }
    }

}
