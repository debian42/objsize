package de.codecoverage.utils;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.TreeMap;

import sun.misc.Unsafe;

/**
 * Estimate size of java object
 * written in the jdk 1.6 - 7+ period  
 */
public class SizeOfObj
{
	private static final String INDENT = "  ";
	private static final String ID_FORMAT = " #%d";
	private static final String REF_ID_FORMAT = "ref#%d";
	private static final String SUPER_PREFIX = "^.";
	private static final String ERROR_FORMAT = "!%s:%s";
	private static final Unsafe UNSAFE = getUnsafe();

	private SizeOfObj() {}

	// ripped somewhere on the Internet
	private static Unsafe getUnsafe() {
		try {
			Field unsafe = Unsafe.class.getDeclaredField("theUnsafe");
			unsafe.setAccessible(true);
			return (Unsafe) unsafe.get(null);
		} catch (Throwable t) {
			throw new IllegalArgumentException(t);
		}
	}

	private static final PrintWriter nirvana = new PrintWriter(new OutputStream() {
		@Override
		public void write(int paramInt) throws IOException { /* ignore */ }
	});

	/**
	 * 
	 * @param in the object to get the estimated memory size 
	 * @param stats a PrintWriter to give some statistics 
	 * @return the object size
	 */
	public static long sizeOf(Object in, PrintWriter stats) {
		return sizeOf(in, nirvana, stats);
	}

	/**
	 * 
	 * @param in the object to get the estimated memory size
	 * @return the object size
	 */
	public static long sizeOf(Object in) {
		return sizeOf(in, nirvana, null);
	}

	/**
	 * Estimate the memory size of a simple Java-Object 
	 * A big ball of mud. IIRC, 
	 * I was angry because everyone was only looking at code through SOLID/CleanCode glasses
	 * 
	 * @param in the object to get the estimated memory size
	 * @param out   PrintWriter to write details 
	 * @param stats PrintWriter to write footprint statistic if not null
	 * @return the object size
	 */
	@SuppressWarnings("boxing")
	public static long sizeOf(Object in, PrintWriter out, PrintWriter stats) {
		class Counter {
			long c = 0;

			Counter(long i) { c = i; }

			void set(long i) { c = i; }
			long get() { return c; }

			@Override
			public int hashCode() {
				return Objects.hash(c);
			}

			@Override
			public boolean equals(Object obj) {
				if (this == obj)				  return true;
				if (obj == null)				  return false;
				if (getClass() != obj.getClass()) return false;
				return true;
			}

			@Override
			public String toString() {
				return String.valueOf(c);
			}
		}

		class Named extends Counter {
			String _name = null;

			Named(String name) {
				super(0);
				_name = name;
			}

			public boolean isTypeEquals(Object obj) {
				if (this == obj)				  return true;
				if (obj == null)				  return false;
				if (getClass() != obj.getClass()) return false;
				return true;
			}

			@Override
			public int hashCode() {
				return Objects.hash(_name);
			}

			@Override
			public boolean equals(Object obj) {
				if (isTypeEquals(obj))			return _name.equals(obj);
				return false;
			}

			@Override
			public String toString() {
				return c + _name;
			}
		}

		class EndArray extends Named {
			EndArray(String name) {
				super(name);
			}
		}

		class EndObject extends Named {
			EndObject(String name) {
				super(name);
			}
		}

		class SizeCounter extends Counter {
			SizeCounter(int i) {
				super(i);
			}
		}

		if (in == null || out == null)
			throw new IllegalArgumentException("in or out is null");

		int idCounter = 0;
		PrintWriter sb = out;
		Deque<Object[]> stack = new ArrayDeque<>();
		IdentityHashMap<Object, Object> found = new IdentityHashMap<Object, Object>();
		TreeMap<String, Field> fieldMap = new TreeMap<String, Field>();
		ArrayList<Entry<String, Field>> fieldList = new ArrayList<Entry<String, Field>>();

		EndArray endArray = new EndArray("endArray");
		EndObject endObject = new EndObject("endObject");
		SizeCounter sizeCounter = new SizeCounter(0);
		StringBuilder emptyString = new StringBuilder();
		stack.push(new Object[] { emptyString, emptyString, in, in.getClass(), sizeCounter });

		long sumSize = 0;
		ArrayList<Named> footprint = new ArrayList<Named>();

		while (!stack.isEmpty()) {
			Object[] params = stack.pop();
			StringBuilder indents = (StringBuilder) params[0];
			StringBuilder label = (StringBuilder) params[1];
			Object next = params[2];
			Class<?> clz = (Class<?>) params[3];
			SizeCounter sc = (SizeCounter) params[4];

			fieldMap = new TreeMap<String, Field>();
			fieldList = new ArrayList<Entry<String, Field>>();
			sb.append(indents).append(label);

			if (next == null) {
				sb.print("null  ~");
				sb.print(ObjSize.getRefSize());
				sb.print('B');
			} else if (clz.isPrimitive()) {
				int s = ObjSize.getSizeNative(clz);
				sc.set(sc.get() + s);
				sb.append(String.valueOf(next));
				sb.append("  ~").print(s);
				sb.append('B');
			} else if (endArray.isTypeEquals(next)) {
				long algSize = ObjSize.align8(sc.get());
				sb.append("]  ~").print(algSize);
				sb.append('B');
				sumSize += algSize;
				EndArray obj = (EndArray) next;
				obj.c = algSize;
				footprint.add(obj);
			} else if (endObject.isTypeEquals(next)) {
				long algSize = ObjSize.align8(sc.get());
				sb.append("}  ~").print(algSize);
				sb.append('B');
				sumSize += algSize;
				EndObject obj = (EndObject) next;
				obj.c = algSize;
				footprint.add(obj);
			} else {
				Integer counter = (Integer) found.get(next);
				if (counter != null) {
					int s = ObjSize.getRefSize();
					sb.append('(').append(String.format(REF_ID_FORMAT, counter)).append(")  ~").print(s);
					sb.append('B');
				} else {
					found.put(next, ++idCounter);
					if (clz.isArray()) {
						SizeCounter _sc = new SizeCounter(ObjSize.getArrayObjSize());
						sb.append('[').append(clz.getSimpleName()).append(':').append(String.format(ID_FORMAT, idCounter));
						int length = Array.getLength(next);
						if (length == 0) {
							long algSize = ObjSize.align8(_sc.get());
							sb.append("]  ~").print(algSize);
							sb.append('B');
							sumSize += algSize;
							EndArray obj = new EndArray(clz.getName());
							obj.c = algSize;
							footprint.add(obj);
						} else {
							stack.push(new Object[] { indents, emptyString, new EndArray(clz.getName()), clz, _sc });
							StringBuilder nextTab = new StringBuilder().append(indents).append(INDENT);
							for (int i = length - 1; i >= 0; i--) {
								Object arrayObject = Array.get(next, i);
								StringBuilder nextLabel = new StringBuilder().append(i).append(") : ");
								if (clz.getComponentType().isPrimitive()) {
									stack.push(new Object[] { nextTab, nextLabel, arrayObject, clz.getComponentType(), _sc });
								} else {
									_sc.set(_sc.get() + ObjSize.getRefSize());
									stack.push(new Object[] { nextTab, nextLabel, arrayObject, arrayObject != null ? arrayObject.getClass() : null, _sc });
								}
							}
						}
					} else {
						SizeCounter _sc = new SizeCounter(ObjSize.getObjSize());
						StringBuilder superPrefix = new StringBuilder();
						for (Class<?> clazz = clz; clazz != null && !clazz.equals(Object.class); clazz = clazz.getSuperclass())
						{
							Field[] fields = clazz.getDeclaredFields();
							for (int i = 0; i < fields.length; i++) {
								Field field = fields[i];
								if (!Modifier.isStatic(field.getModifiers())) {
									fieldMap.put(superPrefix + field.getName(), field);
								}
							}
							superPrefix.append(SUPER_PREFIX);
						}
						for (Entry<String, Field> entry : fieldMap.entrySet()) {
							fieldList.add(entry);
						}

						String nameClass = clz.getSimpleName();
						if (clz.isAnonymousClass())
							nameClass = clz.getName();
						
						sb.append("{").append(nameClass).append(':').append(String.format(ID_FORMAT, idCounter));

						if (fieldList.isEmpty()) {
							long algSize = ObjSize.align8(_sc.get());
							sb.append("]  ~").print(algSize);
							sb.append('B');
							sumSize += algSize;
							EndObject obj = new EndObject(clz.getName());
							obj.c = algSize;
							footprint.add(obj);
						} else {
							stack.push(new Object[] { indents, emptyString, new EndObject(clz.getName()), clz, _sc });
							StringBuilder nextTab = new StringBuilder().append(indents).append(INDENT);
							for (int i = fieldList.size() - 1; i >= 0; i--) {
								Entry<String, Field> entry = fieldList.get(i);
								String name = entry.getKey();
								Field field = entry.getValue();
								Object fieldObject;
								try {
									try {
										fieldObject = field.get(next);
									} catch (Throwable e) {
										field.setAccessible(true);
										fieldObject = field.get(next);
									}
								} catch (Throwable e) {
									try {
										// Idea found on the internet...
										fieldObject = makeItUnsafe(field, next);
									} catch (Throwable err) {
										System.out.println(err);
										fieldObject = String.format(ERROR_FORMAT, err.getClass().getSimpleName(),err.getMessage());
									}
								}
								StringBuilder nextLabel = new StringBuilder().append(name).append(" : ");
								if (field.getType().isPrimitive()) {
									stack.push(new Object[] { nextTab, nextLabel, fieldObject, field.getType(), _sc });
								} else {
									_sc.set(_sc.get() + ObjSize.getRefSize());
									stack.push(new Object[] { nextTab, nextLabel, fieldObject, fieldObject != null ? fieldObject.getClass() : null, _sc });
								}
							}
						}
					}
				}
			}
			if (!stack.isEmpty()) {
				sb.append('\n');
			}
		}
		sb.append("\nObjSize: ~" + sumSize + " Bytes\n");
		sb.flush();
		// print stats
		if (stats != null) {
			class C {
				long _sumSize;
				long _sumCount;
			}
			stats.println();
			LinkedHashMap<String, C> agg = new LinkedHashMap<String, C>();
			Collections.sort(footprint, new Comparator<Named>() {
				@Override
				public int compare(Named a, Named b) {
					return a._name.compareTo(b._name);
				}
			});
			long _sumCount = 0;
			long _sumSize = 0;
			for (Named n : footprint) {
				_sumSize += n.c;
				_sumCount++;
				C v = agg.get(n._name);
				if (v == null) {
					C c = new C();
					c._sumCount = 1;
					c._sumSize = n.c;
					agg.put(n._name, c);
				} else {
					v._sumCount += 1;
					v._sumSize += n.c;
				}
			}
			stats.print(String.format("%-12s%-12s%-12s\n", "Count", "Sum", "DESCRIPTION"));
			for (Entry<String, C> s : agg.entrySet()) {
				C rv = s.getValue();
				stats.print(String.format("%-12s%-12s%-12s\n", rv._sumCount, rv._sumSize, s.getKey()));
			}
			stats.print(String.format("%-12s%-12s%-12s\n", _sumCount, _sumSize, "Sum total"));
			stats.flush();
		}

		return sumSize;
	}

	private static Object makeItUnsafe(Field field, Object obj) {
	    long off = -1;
	    Class<?> type = field.getType();
	    if (Modifier.isStatic(field.getModifiers())) {
	        off = UNSAFE.staticFieldOffset(field);
	    } else {
	        off = UNSAFE.objectFieldOffset(field);
	    }
	    return type.isPrimitive()
	           ? getPrimitiveUnsafe(obj, off, type)
	           : UNSAFE.getObject(obj, off);
	}
	
	private static Object getPrimitiveUnsafe(Object obj, long off, Class<?> type) {
	    if (type == int.class)     return UNSAFE.getInt(obj, off);
	    if (type == long.class)    return UNSAFE.getLong(obj, off);
	    if (type == float.class)   return UNSAFE.getFloat(obj, off);
	    if (type == double.class)  return UNSAFE.getDouble(obj, off);
	    if (type == boolean.class) return UNSAFE.getBoolean(obj, off);
	    if (type == byte.class)    return UNSAFE.getByte(obj, off);
	    if (type == char.class)    return UNSAFE.getChar(obj, off);
	    if (type == short.class)   return UNSAFE.getShort(obj, off);
	    System.err.println("Type: " + type + " not primitive");
	    return null;
	}

	/**
	 * Helper to get some sizes
	 */
	public static final class ObjSize {
		private ObjSize() {}
		private static final boolean isCOOPs = Runtime.getRuntime().maxMemory() < 34359738368L;
		private static final String bits = System.getProperty("sun.arch.data.model");

		//0 = 32 Bit 1 = 64 Bit 2 = 64 Bit comp. oops		 
		public static final byte TYPE_VM = (byte) ("64".equals(bits) ? (isCOOPs == true ? 2 : 1) : 0);

		/*
		 * size[0] = boolean 
		 * size[1] = byte 
		 * size[2] = char 
		 * size[3] = short 
		 * size[4] = int
		 * size[5] = long 
		 * size[6] = float 
		 * size[7] = double 
		 * size[8] = ObjectRef 
		 * size[9] = Object...Mark+ClassWord
		 */
		private static final byte[] natSizes = new byte[] {
				/* 32Bit */      1, 1, 2, 2, 4, 8, 4, 8, 4, 8,
				/* 64Bit */      1, 1, 2, 2, 4, 8, 4, 8, 8, 12 /*since metaspaca always compressed class ptr?*/,
				/* 64BitCOOPs */ 1, 1, 2, 2, 4, 8, 4, 8, 4, 12 };

		private static final byte offsetNative = (byte) (TYPE_VM * 10);

		/*
		 * size[0] = Boolean 
		 * size[1] = Byte 
		 * size[2] = Character 
		 * size[3] = Short 
		 * size[4] = Integer 
		 * size[5] = Long 
		 * size[6] = Float 
		 * size[7] = Double 
		 * size[8] = String
		 * size[8] = Array
		 */
		private static final byte oS = natSizes[offsetNative + 9];

		private static final byte arrayObjSize = (byte) (oS + 4);

		public static long align8(long in) {
			return ((-in) & (8 - 1)) + in;
		}

		public static int getArrayObjSize() {
			return arrayObjSize;
		}

		public static <T> int getSizeNative(Class<T> clz) {
			if (clz.equals(boolean.class))
				return natSizes[offsetNative + 0];

			if (clz.equals(byte.class))
				return natSizes[offsetNative + 1];

			if (clz.equals(char.class))
				return natSizes[offsetNative + 2];

			if (clz.equals(short.class))
				return natSizes[offsetNative + 3];

			if (clz.equals(int.class))
				return natSizes[offsetNative + 4];

			if (clz.equals(long.class))
				return natSizes[offsetNative + 5];

			if (clz.equals(float.class))
				return natSizes[offsetNative + 6];

			if (clz.equals(double.class))
				return natSizes[offsetNative + 7];

			System.err.println("NO match for --> " + clz + " <--");
			return 0;
		}

		public static int getObjSize() {
			return oS;
		}

		public static int getRefSize() {
			return natSizes[offsetNative + 8];
		}
	}
}
