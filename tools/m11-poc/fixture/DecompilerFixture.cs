using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;

namespace DroidInspect.M11.Fixtures;

public interface ICalculator
{
    int Add(int left, int right);
    event EventHandler? Calculated;
}

public abstract class CalculatorBase
{
    public abstract int Add(int left, int right);
}

public sealed class Calculator : CalculatorBase, ICalculator
{
    public int Value { get; private set; }
    public event EventHandler? Calculated;

    public Calculator() { }
    public Calculator(int initialValue) => Value = initialValue;

    public override int Add(int left, int right)
    {
        Value = left + right;
        Calculated?.Invoke(this, EventArgs.Empty);
        return Value;
    }

    public int SumTo(int limit)
    {
        var total = 0;
        for (var i = 0; i <= limit; i++)
            if ((i & 1) == 0) total += i;
        return total;
    }

    public string Describe(int value)
    {
        try
        {
            if (value < 0) throw new ArgumentOutOfRangeException(nameof(value));
            return $"Value={value}";
        }
        catch (ArgumentOutOfRangeException)
        {
            return "negative";
        }
    }

    public T Echo<T>(T value) => value;
    public TResult Map<T, TResult>(T value, Func<T, TResult> map) => map(value);
    public IEnumerable<int> Evens(IEnumerable<int> values) => values.Where(x => x % 2 == 0);
    public async Task<int> AddLaterAsync(int left, int right) =>
        await Task.FromResult(left + right).ConfigureAwait(false);
}

public sealed class Container<T>
{
    public sealed class Nested<U>
    {
        public T First { get; }
        public U Second { get; }
        public Nested(T first, U second) { First = first; Second = second; }
    }
}

public enum FixtureMode { None, Fast, Safe }
public readonly struct Coordinate
{
    public int X { get; }
    public int Y { get; }
    public Coordinate(int x, int y) { X = x; Y = y; }
}

[AttributeUsage(AttributeTargets.Class | AttributeTargets.Method)]
public sealed class FixtureMarkerAttribute : Attribute
{
    public string Name { get; }
    public FixtureMarkerAttribute(string name) => Name = name;
}

[FixtureMarker("entry")]
public static class FixtureEntry
{
    public static int Multiply(int a, int b) => a * b;
    public static Func<int, int> Capture(int offset) => value => value + offset;
}
