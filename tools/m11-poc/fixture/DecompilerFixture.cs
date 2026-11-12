namespace DroidInspect.M11.Fixtures;
public sealed class Calculator
{
 public int Value { get; private set; }
 public int Add(int left, int right) { Value = left + right; return Value; }
 public int SumTo(int limit) { var total=0; for(var i=0;i<=limit;i++) { if((i&1)==0) total+=i; } return total; }
 public string Describe(int value) { try { if(value<0) throw new ArgumentOutOfRangeException(nameof(value)); return $"Value={value}"; } catch(ArgumentOutOfRangeException) { return "negative"; } }
 public T Echo<T>(T value) => value;
}
public sealed class Container<T> { public sealed class Nested<U> { public T First {get;} public U Second {get;} public Nested(T first,U second){First=first;Second=second;} } }
public static class FixtureEntry { public static int Multiply(int a,int b)=>a*b; }
