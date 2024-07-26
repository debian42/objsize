# SizeOfObj
Estimate the size of the Java objects.  
To get accurate estimates, use a tool like [JOL](https://github.com/openjdk/jol)  
This is old code with a big sizeOf() method.   
I found it on an old spinning rust disk and
want to save it here

## Build and Test
execute `test.sh` to run some basic tests, uses `build.sh` to build the stuff.

## Usage
Drop `SizeOfObj.java` somewhere into your package structure(modify the package) and use it like this:

```
public static void main(String... strings)
{
	class Output
	{
		long[] la = { -1 };
		String str = "str";
	}

	PrintWriter out = new PrintWriter(System.out);
	long size = SizeOfObj.sizeOf(new Output(), out, out);
	System.out.println("Size: " + size + " bytes");
}
```
### Output:
```
{Output: #1
  la : [long[]: #2
    0) : -1  ~8B
  ]  ~24B
  str : {String: #3
    coder : 0  ~1B
    hash : 0  ~4B
    hashIsZero : false  ~1B
    value : [byte[]: #4
      0) : 115  ~1B
      1) : 116  ~1B
      2) : 114  ~1B
    ]  ~24B
  }  ~24B
}  ~24B
ObjSize: ~96 Bytes

Count       Sum         DESCRIPTION 
1           24          [B          
1           24          [J          
1           24          de.codecoverage.utils.TestDriver$Output
1           24          java.lang.String
4           96          Sum total   
Size: 96 bytes
```

[Blog](https://www.codecoverage.de/posts/java/objsize/)

