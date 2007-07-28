package java.util;

public class HashMap<K, V> implements Map<K, V> {
  private int size;
  private Cell[] array;
  private final CellFactory factory;

  HashMap(int capacity, CellFactory<K, V> factory) {
    if (capacity > 0) {
      array = new Cell[nextPowerOfTwo(capacity)];
    }
    this.factory = factory;
  }

  public HashMap(int capacity) {
    this(capacity, new MyCellFactory());
  }

  public HashMap() {
    this(0);
  }

  private static int nextPowerOfTwo(int n) {
    int r = 1;
    while (r < n) r <<= 1;
    return r;
  }

  public int size() {
    return size;
  }

  private static int hash(Object a) {
    return (a == null ? 0 : a.hashCode());
  }

  private static boolean equal(Object a, Object b) {
    return (a == null && b == null) || (a != null && a.equals(b));
  }

  private void resize() {
    if (array == null || size >= array.length * 2) {
      resize(array == null ? 16 : array.length * 2);
    } else if (size <= array.length / 3) {
      resize(array.length / 2);
    }
  }

  private void resize(int capacity) {
    Cell<K, V>[] newArray = null;
    if (capacity != 0) {
      capacity = nextPowerOfTwo(capacity);
      if (array != null && array.length == capacity) {
        return;
      }

      newArray = new Cell[capacity];
      if (array != null) {
        for (int i = 0; i < array.length; ++i) {
          Cell<K, V> next;
          for (Cell<K, V> c = array[i]; c != null; c = next) {
            next = c.next();
            int index = c.getKey().hashCode() & (capacity - 1);
            c.setNext(array[index]);
            array[index] = c;
          }
        }
      }
    }
    array = newArray;
  }

  private Cell<K, V> find(K key) {
    if (array != null) {
      int index = hash(key) & (array.length - 1);
      for (Cell<K, V> c = array[index]; c != null; c = c.next()) {
        if (equal(key, c.getKey())) {
          return c;
        }
      }
    }

    return null;
  }

  private void insert(Cell<K, V> cell) {
    ++ size;

    resize();

    int index = cell.hashCode() & (array.length - 1);
    cell.setNext(array[index]);
    array[index] = cell;
  }

  // primarily for use by WeakHashMap:
  void remove(Cell<K, V> cell) {
    int index = cell.hashCode() & (array.length - 1);
    Cell<K, V> p = null;
    for (Cell<K, V> c = array[index]; c != null; c = c.next()) {
      if (c == cell) {
        if (p == null) {
          array[index] = c.next();
        } else {
          p.setNext(c.next());
        }
        -- size;
        break;
      }
    }

    resize();
  }

  private Cell<K, V> putCell(K key, V value) {
    Cell<K, V> c = find(key);
    if (c == null) {
      insert(factory.make(key, value, null));
    } else {
      V old = c.getValue();
      c.setValue(value);
    }
    return c;
  }

  public boolean containsKey(K key) {
    return find(key) != null;
  }

  public boolean containsValue(V value) {
    return values().contains(value);
  }

  public V get(K key) {
    Cell<K, V> c = find(key);
    return (c == null ? null : c.getValue());
  }

  public Cell<K, V> removeCell(K key) {
    Cell<K, V> old = null;
    if (array != null) {
      int index = hash(key) & (array.length - 1);
      Cell<K, V> p = null;
      for (Cell<K, V> c = array[index]; c != null; c = c.next()) {
        if (equal(key, c.getKey())) {
          old = c;
          if (p == null) {
            array[index] = c.next();
          } else {
            p.setNext(c.next());
          }
          -- size;
          break;
        }
      }

      resize();
    }
    return old;
  }

  public V put(K key, V value) {
    Cell<K, V> c = putCell(key, value);
    return (c == null ? null : c.getValue());
  }

  public V remove(K key) {
    Cell<K, V> c = removeCell(key);
    return (c == null ? null : c.getValue());
  }

  public void clear() {
    array = null;
    size = 0;
  }

  public Set<Entry<K, V>> entrySet() {
    return new EntrySet();
  }

  public Set<K> keySet() {
    return new KeySet();
  }

  public Collection<V> values() {
    return new Values();
  }

  Iterator<Entry<K, V>> iterator() {
    return new MyIterator();
  }

  interface Cell<K, V> extends Entry<K, V> {
    public HashMap.Cell<K, V> next();

    public void setNext(HashMap.Cell<K, V> next);
  }

  interface CellFactory<K, V> {
    public Cell<K, V> make(K key, V value, Cell<K, V> next);
  }

  private static class MyCell<K, V> implements Cell<K, V> {
    public final K key;
    public V value;
    public Cell<K, V> next;

    public MyCell(K key, V value, Cell<K, V> next) {
      this.key = key;
      this.value = value;
      this.next = next;
    }

    public K getKey() {
      return key;
    }

    public V getValue() {
      return value;
    }

    public void setValue(V value) {
      this.value = value;
    }

    public HashMap.Cell<K, V> next() {
      return next;
    }

    public void setNext(HashMap.Cell<K, V> next) {
      this.next = next;
    }

    public int hashCode() {
      return (key == null ? 0 : key.hashCode());
    }
  }

  private static class MyCellFactory<K, V> implements CellFactory<K, V> {
    public Cell<K, V> make(K key, V value, Cell<K, V> next) {
      return new MyCell(key, value, next);
    }
  }

  private class EntrySet implements Set<Entry<K, V>> {
    public int size() {
      return HashMap.this.size();
    }

    public boolean contains(Entry<K, V> e) {
      return containsKey(e.getKey());
    }

    public boolean add(Entry<K, V> e) {
      return putCell(e.getKey(), e.getValue()) != null;
    }

    public boolean remove(Entry<K, V> e) {
      return removeCell(e.getKey()) != null;
    }

    public void clear() {
      HashMap.this.clear();
    }

    public Iterator<Entry<K, V>> iterator() {
      return new MyIterator();
    }
  }

  private class KeySet implements Set<K> {
    public int size() {
      return HashMap.this.size();
    }

    public boolean contains(K key) {
      return containsKey(key);
    }

    public boolean add(K key) {
      return putCell(key, null) != null;
    }

    public boolean remove(K key) {
      return removeCell(key) != null;
    }

    public void clear() {
      HashMap.this.clear();
    }

    public Iterator<K> iterator() {
      return new KeyIterator(new MyIterator());
    }
  }


  private class Values implements Collection<V> {
    public int size() {
      return HashMap.this.size();
    }

    public boolean contains(V value) {
      return containsValue(value);
    }

    public boolean add(V value) {
      throw new UnsupportedOperationException();
    }

    public boolean remove(V value) {
      throw new UnsupportedOperationException();
    }

    public void clear() {
      HashMap.this.clear();
    }

    public Iterator<V> iterator() {
      return new ValueIterator(new MyIterator());
    }
  }

  private class MyIterator implements Iterator<Entry<K, V>> {
    private int currentIndex = -1;
    private int nextIndex = -1;
    private Cell<K, V> previousCell;
    private Cell<K, V> currentCell;
    private Cell<K, V> nextCell;

    public MyIterator() {
      hasNext();
    }

    public Entry<K, V> next() {
      if (hasNext()) {
        if (currentCell != null && currentCell.next() != null) {
          previousCell = currentCell;
        } else {
          previousCell = null;
        }

        currentCell = nextCell;
        currentIndex = nextIndex;

        nextCell = nextCell.next();

        return currentCell;
      } else {
        throw new NoSuchElementException();
      }
    }

    public boolean hasNext() {
      if (array != null) {
        while (nextCell == null && ++ nextIndex < array.length) {
          if (array[nextIndex] != null) {
            nextCell = array[nextIndex];
            return true;
          }
        }
      }
      return false;
    }

    public void remove() {
      if (currentCell != null) {
        if (previousCell == null) {
          array[currentIndex] = currentCell.next();
        } else {
          previousCell.setNext(currentCell.next());
          if (previousCell.next() == null) {
            previousCell = null;
          }
        }
        currentCell = null;
      } else {
        throw new IllegalStateException();
      }
    }
  }

  private static class KeyIterator<K, V> implements Iterator<K> {
    private final Iterator<Entry<K, V>> it;

    public KeyIterator(Iterator<Entry<K, V>> it) {
      this.it = it;
    }

    public K next() {
      return it.next().getKey();
    }

    public boolean hasNext() {
      return it.hasNext();
    }

    public void remove() {
      it.remove();
    }
  }

  private static class ValueIterator<K, V> implements Iterator<V> {
    private final Iterator<Entry<K, V>> it;

    public ValueIterator(Iterator<Entry<K, V>> it) {
      this.it = it;
    }

    public V next() {
      return it.next().getValue();
    }

    public boolean hasNext() {
      return it.hasNext();
    }

    public void remove() {
      it.remove();
    }
  }
}