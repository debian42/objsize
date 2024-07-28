package de.codecoverage.utils;

import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;

public final class TestDriver {
	private static final class Output 
	{		
		long[] la = { -1 };
		String str = "str";		
	}
	
	private static final class Test {
		String NULL = null;
		char[] crs = { '1', '2' };
		long[] la = { 1 };
		String str24 = "12222";
		String s1tr24 = "11112";
		String str32 = "12345678";
		String str40 = "123456789";
	}

	private enum E {
		E1("E1", new Object[] { "String1", 1 }),
		E2("E2", new Object[] { "String2", 2 });

		private final String str;
		private final Object[] a;

		E(String str, Object[] a) {
			this.str = str;
			this.a = a;
		}

	}

	private static final class Test1 {
		Test test = new Test();
		Date date = new Date();
		E e1  = E.E1;
		E e2  = E.E2;
		E e11 = E.E1;
		E e22 = E.E2;
		int[][] aaInt = { { 1, 2, 3 }, { 1, 2, 3 }, { 1, 2, 3 }, { 1, 2, 3 } };
		Object[] ooInt = { 
				new Object[] { 1 }, 
				new Object[] { 2, "Zwei" },
				new Object[] { 3, new Object[] { "Drei", "Drei1" } },
				new Object[] { 3, new StringBuilder(new String("StringBuilder,StringBuilder1,StringBuilder2,StringBuilder3-StringBuilder,StringBuilder1,StringBuilder2,StringBuilder3-")) } 
		};
	}

	private static final class TestRefHolder {
		static class Trans implements Serializable{
			private static final long serialVersionUID = -42L;
			transient int aaaaa = 0xffffffff;
    		volatile  int bbbbb = 0xffffffff;
    		volatile  int ccccc = 0xffffffff;
    	  } // 24 Bytes compresses class, 64 VM
		
		Trans trans = new Trans();
		
		/* 
		 * Not working with java.lang.Class  ,
		 * java.lang.invoke.* and some others
		 */		
		// Thread thread = Thread.currentThread(); 
		Object[] a = {};
		Object[] aNull = null;
		AtomicLong atomicLong = new AtomicLong(42);
		Test1 test = new Test1();
		Test1 reftest1 = test;
		ArrayList<Linklist> linkl = new ArrayList<Linklist>();
		LinkedList<Long> lll = new LinkedList<Long>();
		StringBuilder sb = new StringBuilder("FRANKWASHERE");
		
		TestRefHolder() {
			lll.add(1L);	lll.add(2L);
			lll.add(3L);	lll.add(1L);
			lll.add(1L);
			Linklist r = new Linklist();
			linkl.add(r);
			linkl.add(new Linklist());
			linkl.add(new Linklist());
			linkl.add(r);
			hm.put("1", 1L);	hm.put("2", 2L);
			hm.put("3", 3L);	hm.put("4", 4L);
		}

		Object emptyObj = new Object();
		Object[] ooEmptyObjects = { emptyObj, emptyObj, new Object(), new Object(), new Object(), Calendar.getInstance() };
		Calendar cal = Calendar.getInstance();
		HashMap<String, Long> hm = new HashMap<String, Long>();
	}

	private static final class Linklist {
		LinkedList<Object> lll = new LinkedList<Object>();

		Linklist() {
			Date d = new Date();
			lll.add(1);			 lll.add(2);
			lll.add(d);			 lll.add(3);
			lll.add(4L);		 lll.add(1);
			lll.add(2L);		 lll.add(3);
			lll.add(4L);		 lll.add(null);
			lll.add(new Date()); lll.add(new Date());
			lll.add(d);		
		}
	}

	private static final class TestStringDeDupLatin1 
	{
		ConcurrentLinkedDeque<String> str = new ConcurrentLinkedDeque<>();
		TestStringDeDupLatin1() {
			for (int ia = 0; ia < 100; ia++) {
				for (int i = 0; i < 10; i++) {
					str.add(new String("String@Deduplication123" + i));
					// (8 + 4)(Header) + 4(array length) + 24(bytes) = 40 latin1
				}
			} 
				try {
				System.gc();
				Thread.sleep(300);
				System.gc();
				Thread.sleep(300);
				System.gc();
				Thread.sleep(300);
			} catch (InterruptedException e) {
				// ignore
			}
		}
	}
	
	private static final class TestStringDeDupUTF16 {
		ConcurrentLinkedDeque<String> str = new ConcurrentLinkedDeque<>();

		TestStringDeDupUTF16() 
		{
			for (int ia = 0; ia < 100; ia++) {
				for (int i = 0; i < 10; i++) {
					str.add(new String("String€Deduplication123" + i)); // ISO-8859-1(Latin1) has no EURO-Sign => (UTF16)
					// (8 + 4)(Header) + 4(array length) + 48(bytes) = 64 utf16
					}
			}
			try {
				// to consolidate the underlying byte arrays
				// run with -XX:+UseG1GC -XX:+UseStringDeduplication
				System.gc();
				Thread.sleep(300);
				System.gc();
				Thread.sleep(300);
				System.gc();
				Thread.sleep(300);
				/**
				 * With String deduplication 
				 * Count       Sum         DESCRIPTION 
				 * 10          XXX         [B
				 * Without String deduplication
				 * Count       Sum         DESCRIPTION 
				 * 1000        XXXXX       [B
				 */
				
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	
	private enum VMTYPE {
		BIT32,
		BIT64,
		BIT64CP;
		public boolean isEqual(int i) {
			return ordinal() == i;
		}
	}
    
	/**
	 * Test
	 */
	public static void main(String... strings) 
	{
		int type = SizeOfObj.ObjSize.TYPE_VM;
		
        if (VMTYPE.BIT32.isEqual(type))
        	throw new IllegalStateException("Tests only valid for 64BitVM");
        
        PrintWriter out = new PrintWriter(System.out);
        
		long size = SizeOfObj.sizeOf(new byte[] { 0, 1, 2, 3, 4 }, out, out);
		System.out.println("Size: " + size + " bytes");
		if ((size == 24 && VMTYPE.BIT64CP.isEqual(type)) || (size == 24 && VMTYPE.BIT64.isEqual(type))) {
		} else	throw new IllegalStateException("byte[5] failed!");



    	size = SizeOfObj.sizeOf(new Output(), out, out);
		System.out.println("Size: " + size + " bytes");
		if ((size == 96 && VMTYPE.BIT64CP.isEqual(type)) || (size == 112 && VMTYPE.BIT64.isEqual(type))) {
		} else	throw new IllegalStateException("Output failed!");

		TestRefHolder test = new TestRefHolder();
		long sT = System.currentTimeMillis();
		size = SizeOfObj.sizeOf(test, out);
		System.out.println("Size: " + size + " bytes");
		System.out.println("Time: " + (System.currentTimeMillis() - sT) + " ms");
		
		if ((size == 6048 && VMTYPE.BIT64CP.isEqual(type)) || ( (size == 7616/*jdk21 Enum*/ || size == 7600) && VMTYPE.BIT64.isEqual(type))) {
		} else	throw new IllegalStateException("TestRefHolder failed!");
		
		
		TestStringDeDupUTF16 dedupUtf = new TestStringDeDupUTF16();
		size = SizeOfObj.sizeOf(dedupUtf, out);
		System.out.println("Size: " + size + " bytes");
		
		if ((size == 48704 && VMTYPE.BIT64CP.isEqual(type)) || (size == 72736 && VMTYPE.BIT64.isEqual(type))) {
		} else	throw new IllegalStateException("TestStringDeDupUTF16 failed!");
		
		TestStringDeDupLatin1 dedupLatin1 = new TestStringDeDupLatin1();
		size = SizeOfObj.sizeOf(dedupLatin1, out);
		System.out.println("Size: " + size + " bytes");
		
		if ((size == 48464 && VMTYPE.BIT64CP.isEqual(type)) || (size == 72496 && VMTYPE.BIT64.isEqual(type))) {
		} else	throw new IllegalStateException("TestStringDeDupLatin1 failed!");
		
		System.out.println("OKAY");
	}

}
