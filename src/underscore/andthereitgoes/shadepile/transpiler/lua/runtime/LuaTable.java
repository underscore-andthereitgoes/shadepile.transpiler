package underscore.andthereitgoes.shadepile.transpiler.lua.runtime;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;


public class LuaTable implements LuaTableOrUserdata {

  private @Nullable LuaTableOrUserdata metatable = null;

  private @Nullable Object @NotNull [] list;
  private final @NotNull Hashtable<Object, Object> table;

  private boolean isReadOnly;
  public boolean isReadOnlyForLua;

  private LuaTable(Object[] list, Hashtable<Object, Object> table) {
    this.list = list;
    this.table = table;
  }

  public LuaTable() {
    this(new Object[0], new Hashtable<>());
  }

  /// Returns a shallow copy of this table (with the same metatable object and writing enabled).
  @Contract(value = "-> new", pure = true)
  public LuaTable copy() {
    var lt = new LuaTable();
    lt.metatable = this.metatable;
    lt.putAll(this);
    return lt;
  }

  @Contract("-> this")
  public LuaTable readonlyInLua() {
    this.isReadOnlyForLua = true;
    return this;
  }

  @Contract("-> this")
  public LuaTable readonly() {
    this.isReadOnly = true;
    return this;
  }

  @Contract("-> this")
  public LuaTable readwrite() {
    this.isReadOnly = false;
    return this;
  }

  public static LuaTable ofList(Object[] list) {
    return new LuaTable(list, new Hashtable<>());
  }

  public static LuaTable ofListN(Object[] list) {
    return new LuaTable(list, new Hashtable<>(Map.of("n", list.length)));
  }

  private static final long MAX_LIST_LENGTH = Integer.MAX_VALUE;
  private static final long MAX_LIST_EXTENSION = 4L; // maximum distance from current length to add new items to list and leave gaps

  @Contract(value = "null -> fail", pure = true)
  private Object correctKey(Object key) {
    if (key == null || (key instanceof Double dk && dk.isNaN())) throw new LuaRuntimeError("cannot use " + (key == null ? "nil" : "nan") + " as a table key");
    if (key instanceof Double dk && dk >= (double)Long.MIN_VALUE && dk <= (double)Long.MAX_VALUE && dk == Math.floor(dk)) return dk.longValue();
    return key;
  }

  private boolean isInList(Object correctedKey) {
    return correctedKey instanceof Long longKey && longKey >= 1L && longKey <= this.list.length;
  }

  private boolean couldBeInList(Object correctedKey) {
    return correctedKey instanceof Long longKey && longKey >= 1L && longKey <= MAX_LIST_LENGTH && longKey <= this.list.length + MAX_LIST_EXTENSION;
  }

  @Override
  public long len() {
    return this.list.length;
  }

  @Override
  public @Nullable LuaTableOrUserdata getMetatable() {
    return this.metatable;
  }

  @Override
  public void setMetatable(@Nullable LuaTableOrUserdata metatable) {
    if (this.isReadOnly) return;
    this.metatable = metatable;
  }

  @Override
  public boolean isEmpty() {
    return this.list.length == 0 && this.table.isEmpty();
  }

  @Override
  @Contract(value = "null -> fail", pure = true)
  public boolean containsKey(Object key) {
    key = correctKey(key);
    return isInList(key) ? this.list[(int)(long)(Long)key - 1] != null : this.table.containsKey(key);
  }

  @Override
  @Contract(value = "null -> false", pure = true)
  public boolean containsValue(Object value) {
    return value != null && Arrays.asList(this.list).contains(value) || this.table.containsValue(value);
  }

  @Override
  @Contract(value = "null -> fail", pure = true)
  public Object get(Object key) {
    key = correctKey(key);
    if (isInList(key)) return this.list[(int)(long)(Long)key - 1];
    return this.table.get(key);
  }

  synchronized public void putFunction(Object key, Function<Object[],Object[]> function) {
    if (this.isReadOnly) return;
    this.put(key, function);
  }

  @Override
  @Contract(value = "null, _ -> fail")
  synchronized public @Nullable Object put(Object key, Object value) {
    if (value == null) return this.remove(key);
    key = correctKey(key);
    if (this.isReadOnly) return this;
    if (couldBeInList(key)) {
      int k = (int)(long)(Long)key;
      Object v0 = this.list[k - 1];
      int idx = k - 1;
      if (this.list.length < k) {
        Object[] list0 = this.list;
        this.list = new Object[k];
        System.arraycopy(list0, 0, this.list, 0, list0.length);

        // find elements between old length and new length
        // old last element (list0.length - 1) is always going to be missing (because it would have been added to this.list)
        // new last element (idx) is going to be set afterwards
        for (int i = list0.length; i < idx; i++) {
          this.list[i] = this.table.remove((long)i+1L);
        }
      }
      this.list[idx] = value;
      return v0;
    }
    return this.table.put(key, value);
  }

  @Override
  @Contract(value = "null -> fail")
  synchronized public Object remove(Object key) {
    key = correctKey(key);
    if (this.isReadOnly) return this;
    if (isInList(key)) {
      int k = (int)(long)(Long)key - 1;
      Object v0 = this.list[k];
      if (k == this.list.length - 1) {
        // trim excess elements;
        // this could trim all the way to the start if you delete every element except the last one before deleting the final elements;
        // find the rightmost non-empty element starting at the current `k`, and this becomes the new last index (and length = last index + 1)
        do k--;
        while (k >= 0 && this.list[k] == null);
        // key (new last index) is now a number from -1 to (this.list.length - 2)
        this.list = Arrays.copyOf(this.list, k);
      } else {
        // leave gap
        this.list[k] = null;
      }
      return v0;
    }
    return this.table.remove(key);
  }

  @Override
  synchronized public void clear() {
    if (this.isReadOnly) return;
    this.list = new Object[0];
    this.table.clear();
  }

  @Override
  public @NotNull Set<Object> keySet() {
    Set<Object> keys = new HashSet<>();
    for (int idx = 0; idx < this.list.length; idx++) if (this.list[idx] != null) keys.add((long)idx);
    keys.addAll(this.table.keySet());
    return keys;
  }

  @Override
  public @NotNull Collection<Object> values() {
    Set<Object> values = new HashSet<>();
    for (Object v: this.list) if (v != null) values.add(v);
    values.addAll(this.table.values());
    return values;
  }

  @Override
  public @NotNull Set<Entry<Object,Object>> entrySet() {
    Set<Entry<Object,Object>> entries = new HashSet<>();
    for (int idx = 0; idx < this.list.length; idx++) {
      Object v = this.list[idx];
      if (v != null) entries.add(new LuaTableListEntry(idx, v));
    }
    entries.addAll(this.table.entrySet());
    return entries;
  }

  public static class LuaTableListEntry implements Map.Entry<Object,Object> {
    private final @NotNull Object key;
    private final @NotNull Object value;
    protected LuaTableListEntry(@NotNull Object key, @NotNull Object value) {
      this.key = key;
      this.value = value;
    }
    @Override
    public @NotNull Object getKey() { return key; }
    @Override
    public @NotNull Object getValue() { return value; }
    @Override
    @Contract(value = "_ -> fail", pure = true)
    public Object setValue(Object value) { throw new UnsupportedOperationException(); }
  }
}
