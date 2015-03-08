# yield-java
Implementation of c#'s Yield to Java. Using threads

```java
ArrayList<Integer> results = new ArrayList<>();
for (Integer number : oneToFive()) results.add(number);
assertEquals(asList(1,2,3,4,5), results);

public static Yielderable<Integer> oneToFive() {
    return yield -> {
        for (int i = 1; i < 10; i++) {
            if (i == 6) yield.breaking();
            yield.returning(i);
        }
    };
}
```
