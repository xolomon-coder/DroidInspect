using System;
using System.Collections.Generic;
using System.Linq;

namespace DroidInspect.M11.ExeFixture;

public interface IRunner
{
    int Run(IEnumerable<int> values);
}

public sealed class Program : IRunner
{
    public static int Main(string[] args)
    {
        var runner = new Program();
        return runner.Run(new[] { 1, 2, 3, 4 });
    }

    public int Run(IEnumerable<int> values) =>
        values.Where(value => value > 1).Sum();

    public async Task<string> DescribeAsync(string value) =>
        await Task.FromResult("EXE:" + value).ConfigureAwait(false);
}
