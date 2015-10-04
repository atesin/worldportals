package cl.netgamer.worldportals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
Class to format vertically aligned text tables for minecraft chat area and server console. 
GPLv3 sep2013 by atesin#gmail,com, optimized version 6 based on v5 used in MyPortals plugin. 
Also based on Erine's implementation which has additional features (http://ee5.net/?p=520).<br/><br/>

USAGE:
<ul>
<li> Build a csv like text, with fields delimited by tabs (\t) and lines by newlines (\n)</li>
<li> This class supports format codes EXCEPT BOLD FORMAT, also USE UPPERCASE CODES or may break alignments</li>
<li> More info: <i>http://minecraft.gamepedia.com/Formatting_codes</i></li>
</ul><ul>
<li> With this text create a TextTab object, specifying default format code and column widths (line = 53chars)</li>
<li> Specify page height and get resulting number of pages</li>
<li> Retrieve some page, for server console or chat area output, fixed or variable width fonts</li>
</ul>
<pre>String myText = "HEAD1\tHEAD2\tHEAD3\n" + "\u00A7Cdata1\tdata2\tdata3";
TextTable myTable = new TextTable(myText, "\u00A7E", 10, 15); // last column width not needed
int numPages = myTable.setPageHeight((int) linesPerPage);
String textForConsole = myTable.getPage(0, true); // 0 = all pages
String textForChat = myTable.getPage(1, false);</pre>

MULTILANGUAGE:<br/><br/>

This class is (hopely) ready for: english, spanish, portuguese, french, german and italian. 
Most latin-ascii based languages may be supported (european?, russian?). 
More info: https://en.wikipedia.org/wiki/List_of_languages_by_number_of_native_speakers. 
If there are some latin based characters that displays incorrectly just tell me. 
In the meanwhile, this class provides a method to add custom chars, use it right after constructing.<br/>

<pre>myTable.addChars(4, "\u0049\u0074");
myTable.addChars(5, "\u0066\u006B");</pre>

VERTICAL ALIGNED TEXT WITH ANY CHARACTER (ORIENTAL, ARABIC, SYMBOLS, ETC.)<br/><br/>

If you feel motivated could write a class that write column texts in ANY unicode character. 
Write a class (base on this if you want) that reads "glyph_sizes.bin" took from minecraft dir. 
First find its "obfuscated" name in <minecraft dir>/assets/indexes/1.8.json<br/><br/>

The file is easy, the position in the file matches the unicode code points. 
From each byte in the file, the 4 bits lsb and msb are the left and right character boundaries. 
The difference beetween boundaries are the character width!, test it with MinecraftFontEditor. 
If you got interested and write some code please show me :D<br/><br/>

@version 6
@author atesin#gmail,com
*/

public class TextTable
{
	private Map<Integer, String> chars = new HashMap<Integer, String>();
	private String[] lines;
	private String format;
	private List<Integer> tabs = new ArrayList<Integer>();
	private int height;
	private int pages;
	
	public TextTable(String text, String format, int... tabs)
	{
		// init chars
		chars.put(-6, "\u00A7");
		chars.put(2, "!.,:;i|"+"\u00A1");
		chars.put(3, "'`l"+"\u00ED\u00CE");
		chars.put(4, " I[]t"+"\u00CD");
		chars.put(5, "\"()*<>fk{}"+"\u00AB\u00BB");
		chars.put(7, "@~");
		
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
		
		// done
		sort();
	}
	
	// add some chars and width to the list
	void addChars(int width, String chars)
	{
		if (!this.chars.containsKey(width))
			this.chars.put(width, "");
		this.chars.get(width).concat(chars);
	}
	
	// sort the lines ignoring format codes
	private void sort()
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
		String table = "";
		for (int l = from; l < to; l++)
		{
			// initialize line, skip empty lines
			table += l == from ? "" : "\n";
			if (lines[l].isEmpty())
				continue;
			
			// loop fields
			String[] fields = lines[l].split("\t");
			for (int f = 0; f < fields.length; f++)
			{
				// basic trim field support
				String field = fields[f];
				int tab = tabs.get(f);
				if (field.length() > tab)
					field = field.substring(0, tab);
				
				// add default formatted field to line, if fields left
				table += format+field;
				if (f >= tabs.size())
					break;
				
				// for fixed width fonts, add spaces to fill width
				if (monospace)
					for (int i = tab - pxLen(field, true); i >= 0; --i)
						table += " ";
				// else add grey 1px spaces and normal 4px spaces
				else
				{
					int fill = (tab * 6) - pxLen(field, false);
					table += "\u00A78";
					for (int i = 0; i < fill % 4; i++)
						table += "\u205A";
					for (int i = 0; i < fill / 4; i++)
						table += " ";
				}
			}
		}
		// return resulting table
		return table;
	}
	
	// calculate the length of a string in pixels (the core private method)
	private int pxLen(String word, boolean monospace)
	{
		// if fixed width fonts, trim format codes and return fixed length
		if (monospace)
			return word.replaceAll("\u00A7.", "").length();

		// else loop word each character searching widths
		int ln = 0;
		for (char ch : word.toCharArray())
		{	
			// loop fixed characters list with default 6 pixels width
			int l = 6;
			for (int px : chars.keySet())
				// if character found in this list add width to line
				if (chars.get(px).indexOf(ch) >= 0)
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
