namespace UnsignedRenameFixture
{
    public sealed class RenameTarget
    {
        public int GetValue()
        {
            return 42;
        }

        public string GetLabel()
        {
            return "DroidInspect rename test";
        }

        public int AddValues(int left, int right)
        {
            return left + right;
        }
    }
}
