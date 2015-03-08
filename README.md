# yield-java
Implementation of c#'s Yield to Java. Using threads

Based on Jim Blackler's implementation http://jimblackler.net/blog/?p=61

Updated to take advantage of Java 8 features.

```java
@Test public void example() {
    ArrayList<Integer> results = new ArrayList<>();
    for (Integer number : oneToFive()) results.add(number);
    assertEquals(asList(1,2,3,4,5), results);
}

public static Yielderable<Integer> oneToFive() {
    return yield -> {
        for (int i = 1; i < 10; i++) {
            if (i == 6) yield.breaking();
            yield.returning(i);
        }
    };
}
```


```java
public static Yielderable<String> fooBar(SideEffects sideEffects) {
   return yield -> {
       sideEffects.sideEffect(1);
       yield.returning("foo");
       sideEffects.sideEffect(2);
       yield.returning("bar");
       sideEffects.sideEffect(3);
   };
}

@Test public void example() {
   SideEffects sideEffects = mock(SideEffects.class);

   Iterable<String> strings = fooBar(sideEffects);

   verifyZeroInteractions(sideEffects);

   assertEquals("foo", strings.iterator().next());

   verify(sideEffects).sideEffect(1);
   verifyNoMoreInteractions(sideEffects);
}
```