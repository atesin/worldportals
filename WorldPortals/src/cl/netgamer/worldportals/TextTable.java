package cl.netgamer.worldportals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
<pre>
@version 7
@author atesin#gmail,com

Class to format vertically aligned text tables for minecraft chat area and server console. 
GPLv3 sep2013, some ideas from Erine's implementation which has additional features (http://ee5.net/?p=520).

RECENT CHANGES

- Variable lengths and format codes optimizations, no more uppercase code required (but still no support for bold)
- Right alignment added, with negative tab value

USAGE:

- Build a csv like text, with fields delimited by tabs (\t) and lines by newlines (\n).
- This class supports format codes EXCEPT BOLD FORMAT or may break alignments.
- More info: <a>http://minecraft.gamepedia.com/Formatting_codes</a>.

- With this text create a TextTab object, specifying default format code and column widths (line = 53chars)
- Specify page height and get resulting number of pages
- Retrieve some page, for server console or chat area output, fixed or variable width fonts

<i>
String myText = "HEAD1\tHEAD2\tHEAD3\n" + "\u00A7Cdata1\tdata2\tdata3";
TextTable myTable = new TextTable(myText, "\u00A7E", 10, 15); // no last column width = left aligned until the end
int numPages = myTable.setPageHeight((int) linesPerPage);
String textForConsole = myTable.getPage(0, true); // 0 = all pages
String textForChat = myTable.getPage(1, false);
</i>

MULTILANGUAGE:

This class is (hopely) ready for: english, spanish, portuguese, french, german and italian. 
Most latin-ascii based languages may be supported (european?, russian?). 
More info: https://en.wikipedia.org/wiki/List_of_languages_by_number_of_native_speakers. 
If there are some latin based characters that displays incorrectly just tell me. 
In the meanwhile, this class provides a method to add custom chars, use it right after constructing

<i>
myTable.addChars(4, "\u0049\u0074");
myTable.addChars(5, "\u0066\u006B");
</i>

VERTICAL ALIGNED TEXT WITH ANY CHARACTER (ORIENTAL, ARABIC, SYMBOLS, ETC.)

If you feel motivated could write a class that write column texts in ANY unicode character. 
Write a class (base on this if you want) that reads "glyph_sizes.bin" took from minecraft dir. 
First find its "obfuscated" name in <minecraft dir>/assets/indexes/1.8.json

The file is easy, the position in the file matches the unicode code points. 
From each byte in the file, the 4 bits lsb and msb are the left and right character boundaries. 
The difference beetween boundaries are the character width!, test it with MinecraftFontEditor. 
If you got interested and write some code please show me :D
</pre>
*/

public class TextTable
{
	//private Map<Integer, String> ch = new HashMap<Integer, String>();
	private String[] ch = new String[8];
	private String[] sp = new String[12];
	private String[] lines;
	private String format;
	private List<Integer> tabs = new ArrayList<Integer>();
	private int height;
	private int pages;
	private boolean monoSpace;
	
	TextTable(String text, String format, int... tabs)
	{
		// init chars
		ch[0] = "";
		ch[1] = "";
		ch[2] = "!.,:;i|"+"\u00A1";
		ch[3] = "'`l"+"\u00ED\u00CE";
		ch[4] = " I[]t"+"\u00CD";
		ch[5] = "\"()*<>fk{}"+"\u00AB\u00BB";
		ch[6] = "";
		ch[7] = "@~";
		
		// init spaces
		sp[0] = "";
		sp[1] = "\u00A78\u205A";
		sp[2] = "\u00A78\u205A\u205A";
		sp[3] = "\u00A78\u205A\u205A\u205A";
		sp[4] = " ";
		sp[5] = "\u00A7L ";
		sp[6] = "\u00A78\u205A\u00A7L ";
		sp[7] = "\u00A78\u205A\u205A\u00A7L ";
		sp[8] = "  ";
		sp[9] = " \u00A7L ";
		sp[10] = "\u00A7L  ";
		sp[11] = "\u00A78\u205A\u00A7L  ";
		
		// init lines
		lines = text.split("[\r\n]+");
		pages = setPageHeight(10);
		this.format = "\u00A7R"+format;
		
		// init tabs
		int last = 53;
		for (int t : tabs)
		{
			this.tabs.add(t);
			last -= t;
		}
		this.tabs.add(last);
		
		// optionally sort
		sort();
	}
	
	// add some chars and width to the list
	void addChars(int width, String chars)
	{
		ch[width].concat(chars);
	}
	
	// sort the lines ignoring format codes
	void sort()
	{
		Arrays.sort(lines, new Comparator<String>()
		{
			@Override
			public int compare(String s1, String s2)
			{
				return s1.replaceAll("\u00A7.", "").compareTo(s2.replaceAll("\u00A7.", ""));
			}
		});
	}

	// set page height and retrieve resulting number of pages
	int setPageHeight(int height)
	{
		this.height = height;
		pages = (int) Math.ceil((double) lines.length / height);
		return pages;
	}
	
	// get some specified page, for console or chat (the core public method)
	String getPage(int page, boolean monospace)
	{
		// define lines range
		int from = 0;
		int to = lines.length;
		if (page > 0)
		{
			from = (Math.min(page, pages) - 1) * height;
			to = Math.min(from + height, lines.length);
		}
		
		// initialize table, loop selected lines
		this.monoSpace = monospace;
		String table = "";
		for (int l = from; l < to; l++)
		{
			// initialize line, skip empty lines
			table += l == from ? "" : "\n";
			if (lines[l].trim().isEmpty())
				continue;
			
			// loop fields
			String[] fields = lines[l].split("\t");
			for (int f = 0; f < fields.length; f++)
			{
				// get field parameters
				String field = fields[f];
				boolean leftHand = true;
				int tab = tabs.get(f);
				if (tab < 0)
				{
					tab = -tab;
					leftHand = false;
				}
				
				// add formatted field to line if lefts
				if (f < tabs.size())
					table += formatField(field, tab, leftHand);
				else
				{
					table += format+field;
					break;
				}
			}
		}
		// return resulting table
		return table;
	}
	
	// returns the given field, adjusted, formatted and aligned
	private String formatField(String field, int tab, boolean leftHand)
	{
		// basic trim to avoid field overflow
		if (field.length() > tab)
			field = field.substring(0, tab);
		
		if (!monoSpace)
			tab *= 6;
		
		String blank = blankSpaces(tab - pxLen(field), leftHand);
		
		if (leftHand)
			return format+field+blank;
		return blank+format+field;
	}
	
	// return blank fill
	private String blankSpaces(int len, boolean leftHand)
	{
		String blank = "";
		
		// fixed width
		if (monoSpace)
			for (int i = 0; i < len; ++i)
				blank += " ";
		
		// variable width less than 12 pixels
		else if (len < 12)
		{
			blank = sp[len];
			
			// special right aligned cases
			if (!leftHand)
			switch (len)
			{
			case 6:
				blank = "\u00A7L \u00A7R\u00A78\u205A";
				break;
			case 7:
				blank = "\u00A7L \u00A7R\u00A78\u205A\u205A";
				break;
			case 11:
				blank = "\u00A7L  \u00A7R\u00A78\u205A";
			}
		}
		
		// variable width from 12 pixels
		else
		{
			int px5 = len % 4;
			int px4 = (len / 4) - px5;
			
			for (int i = 0; i < px4; ++i)
				blank += " ";
			blank += "\u00A7L";
			for (int i = 0; i < px5; ++i)
				blank += " ";
		}
		
		return blank;
	}
	
	// calculate the length of a string in pixels (the core private method)
	private int pxLen(String sentence)
	{
		String stripped = sentence.replaceAll("\u00A7.", "");
		
		// if fixed width fonts, trim format codes and return fixed length
		if (monoSpace)
			return stripped.length();

		// else loop word each character searching widths
		int ln = 0;
		for (char c : stripped.toCharArray())
		{	
			// loop fixed characters list with default 6 pixels width
			int l = 6;
			for (int px = 1; px < 8; ++px)
				// if character found in this list add width to line
				if (ch[px].indexOf(c) >= 0)
				{
					l = px;
					break;
				}
			ln += l;
		}
		// return the resulting lenght
		return ln;
	}
}
