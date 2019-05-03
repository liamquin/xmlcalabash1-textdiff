package com.delightfulcomputing.xproc;

import com.xmlcalabash.core.XMLCalabash;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.library.DefaultStep;
import com.xmlcalabash.runtime.XAtomicStep;
import com.xmlcalabash.util.Base64;
import com.xmlcalabash.util.S9apiUtils;
// import com.xmlcalabash.util.XProcURIResolver;
import com.xmlcalabash.util.TreeWriter;


import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;

import java.util.*;
// import java.util.List;
// import java.util.LinkedList;
// import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import java.util.stream.Collectors; // collect

// for stripping whitespace:
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xml.sax.XMLReader;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.XMLReaderFactory;
import javax.xml.transform.sax.SAXSource;

// for java-diff-utils-jycr
// import difflib.Delta;
// import difflib.Chunk;
// import difflib.Patch;
// import difflib.DiffUtils;
// import difflib.DiffRow;
// import difflib.DiffRowGenerator;

// for java-diff-utils
import com.github.difflib.*;
import com.github.difflib.text.*;
import com.github.difflib.patch.*;

@XMLCalabash(
    name = "dc:textdiff",
    type = "{http://www.delightfulcomputing.com/ns/}textdiff"
)

public class TextDiff extends DefaultStep {

    // private static final String library_xpl = "ttps://www.delightfulcomputing.com/xproc/xpl/textdiff.xpl";
    // private static final String library_url = "/com/delightfulcomputing/extensions/textdiff/library.xpl";

    private ReadablePipe sourcePort = null;
    private WritablePipe resultPipe = null;

    private Boolean sideBySideMode = false;
    private Boolean ignoreSpaceMode = false;

    // Instance variables

    // don't care if we have no input
    //
    public List<String> original = null;
    public List<String> revised  = null;

    public TextDiff(XProcRuntime runtime, XAtomicStep step) {
        super(runtime, step);
    }

    public void setInput(String port, ReadablePipe pipe) throws XProcException {
	if (port.equals("source")) {
	    sourcePort = pipe;
	} else {
	    throw new XProcException(
		"textdiff: unknown port name " + port + " - expected source"
	    );
	}
    }

    public void setOutput(String port, WritablePipe pipe) {
        resultPipe = pipe;
    }

    public void reset() {
        sourcePort.resetReader();
        resultPipe.resetWriter();
    }

    public static final Pattern WHITESPACE_ALL_BYEBYE = Pattern.compile("[\\t ]+");

    private static String spacetrim(String s)
    {
	return WHITESPACE_ALL_BYEBYE.matcher(s.trim()).replaceAll("");
    }

    private List<String> listCopyWithoutSpaces(
	    List<String> harry
	    )
    {
	if (!ignoreSpaceMode) {
	    return harry;
	}

	List<String> result =
	    harry.stream().map(
		    TextDiff::spacetrim
		    ).collect(Collectors.toList());
	return result;
    }

    private static String deltaType(Delta<String> delta) {
	if (delta == null) {
	    return "null";
	}

	return delta.getType().name().toLowerCase();
    }

    private static String rowType(DiffRow row) {
	if (row == null) {
	    return "null";
	}

	return row.getTag().name().toLowerCase();
    }

    // QName xml_base = new QName("xml", "http://www.w3.org/XML/1998/namespace" ,"base");
    static private final QName c_textdiff = new QName("dc", "http://www.delightfulcomputing.com/ns/" ,"textdiff");
    static private final QName c_row = new QName("dc", "http://www.delightfulcomputing.com/ns/" ,"row");
    static private final QName c_delta = new QName("dc", "http://www.delightfulcomputing.com/ns/" ,"delta");
    static private final QName c_orig = new QName("dc", "http://www.delightfulcomputing.com/ns/" ,"original");
    static private final QName c_rev = new QName("dc", "http://www.delightfulcomputing.com/ns/" ,"revised");
    static private final QName c_chunk = new QName("dc", "http://www.delightfulcomputing.com/ns/" ,"chunk");

    private static String getTextFromPort(ReadablePipe source)
	throws SaxonApiException
    {
	XdmNode doc = source.read();
	XdmNode root = S9apiUtils.getDocumentElement(doc);

	final QName _content_type = new QName("content-type");

        if ((XProcConstants.c_data.equals(root.getNodeName())
                && "application/octet-stream".equals(root.getAttributeValue(_content_type)))
                || "base64".equals(root.getAttributeValue(_encoding))) {
            byte[] decoded = Base64.decode(root.getStringValue());
            return new String(decoded);
        } else {
            return root.getStringValue();
        }
    }

    private void chunkToXML(
	    TreeWriter tree,
	    QName elem,
	    Chunk<String> chunk,
	    List<String> unstripped
    ) throws SaxonApiException {
	// we want to make,
	// <delta type="original">
	//   <chunk position="3" lines="2">
	//     <line>stuff</line>
	//     <line>stuff</line>
	//   </chunk>
	// </delta>
	tree.addStartElement(elem);
	tree.addAttribute(new QName("position"), Integer.toString(chunk.getPosition()));
	tree.addAttribute(new QName("size"), Integer.toString(chunk.size()));

	ListIterator<String> eachLine = chunk.getLines().listIterator();
	int p = chunk.getPosition();
	while (eachLine.hasNext()) {
	    tree.addStartElement(c_chunk);
	    String s = eachLine.next();
	    if (ignoreSpaceMode) {
		s = unstripped.get(p);
		p++;
	    }
	    tree.addText(s);
	    tree.addEndElement();
	}
	tree.addEndElement(); // c_rev
    }

    private static Patch<String> restoreSpaces(
	    Patch<String> patch,
	    List<String> original,
	    List<String> revised
    ) throws SaxonApiException
    {
	return patch;
    }

    private XdmNode rowsToXML(
	    List<DiffRow> rows,
	    XProcRuntime runtime,
	    String wrapcols
    ) throws SaxonApiException {
	// Make a simple XML structure
	TreeWriter tree = new TreeWriter(runtime);
        tree.startDocument(null); // argument is base URI
        tree.addStartElement(c_textdiff);
	tree.addAttribute(new QName("ignore-spaces"), ignoreSpaceMode.toString());
	tree.addAttribute(new QName("diff-mode"), "side-by-side");

	if (wrapcols != null) {
	    // We don't actually want to wrap but to truncate.
	    // The side by side diff can wrap, i think.
	    tree.addAttribute(new QName("maxwidth"), wrapcols);
	}

	for (DiffRow row: rows) {
	    tree.addStartElement(c_row); {
		tree.addAttribute(new QName("tag"), rowType(row));

		if (row.getOldLine() != null) {
		    tree.addStartElement(c_orig);
			tree.addText(row.getOldLine());
		    tree.addEndElement();
		}

		if (row.getNewLine() != null) {
		    tree.addStartElement(c_rev);
			tree.addText(row.getNewLine());
		    tree.addEndElement();
		}

	    } tree.addEndElement();
	} // for

        tree.endDocument();
        return tree.getResult();
    } // rowsToXML

    private XdmNode patchToXML(
	    Patch<String> patch,
	    XProcRuntime runtime
    ) throws SaxonApiException {
	// Make a simple XML structure
	TreeWriter tree = new TreeWriter(runtime);
        tree.startDocument(null); // argument is base URI
        tree.addStartElement(c_textdiff);

	for (Delta<String> delta: patch.getDeltas()) {
	    tree.addStartElement(c_delta);
            tree.addAttribute(new QName("type"), deltaType(delta));

	    Chunk<String> orig = delta.getOriginal();
	    if (orig != null) {
		chunkToXML(tree, c_orig, orig, original);
	    }

	    Chunk<String> rev = delta.getRevised();
	    if (rev != null) {
		chunkToXML(tree, c_rev, rev, revised);
	    }

	    tree.addEndElement(); // c_delta
        }
        tree.addEndElement();
        tree.endDocument();
        return tree.getResult();
    } // patchToXML

    @Override
    public void run() throws SaxonApiException {
	super.run();

	/* attributes */
	QName _original_text = new QName("", "original-text");
	QName _revised_text = new QName("", "revised-text");

	String s = null;

	Boolean usedPort = false;

	s = getOption(_original_text, (String) null);
	if (s != null) {
	    original = new ArrayList<String>(Arrays.asList(
			s.split("\r?[\n\u0085\u2028\u2029]")));
	} else {
	    // original-text arrtibute not given, try for source port
	    s = getTextFromPort(sourcePort);
	    usedPort = true;

	    if (s != null) {
		original = new ArrayList<String>(Arrays.asList(
			    s.split("\r?[\n\u0085\u2028\u2029]")));
	    } else {
		// can't find any input
		throw new XProcException(
		    "TextDiff: supply an original-text attribute, or an input <dc:text>...</dc:text> document on the source port."
		);
	    }
	}

	// now the same for revised
	s = getOption(_revised_text, (String) null);
	if (s != null) {
	    revised = new ArrayList<String>(Arrays.asList(
			s.split("\r?[\n\u0085\u2028\u2029]")));
	} else if (!usedPort) {
	    s = getTextFromPort(sourcePort);
	    if (s != null) {
		revised = new ArrayList<String>(Arrays.asList(
			    s.split("\r?[\n\u0085\u2028\u2029]")));
	    } else {
		throw new XProcException(
		    "TextDiff: supply both original-text and revised-text attributes; the source port can replace either one of them"
		);
	    }
	} else {
	    throw new XProcException(
		"TextDiff: supply both original-text and revised-text attributes; the source port can replace only one of them"
	    );
	}
	
	String maxWidth = getOption(new QName("", "max-width"), "132");
	if (!maxWidth.matches("^\\d+$")) {
	    throw new XProcException(
		"TextDiff: max-width must be an integer, not "
		+ maxWidth
	    );
	}

	String mode = getOption(new QName("", "diff-mode"), "default");
	if (mode != null) {
	    if (mode.equals("side-by-side")) {
		sideBySideMode = true;
	    } else if (!mode.equals("default")) {
		throw new XProcException(
		    "TextDiff: mode must be default or side-by-side, not "
		    + mode
		);
	    }
	}

	{
	    String tmp = getOption(new QName("", "ignore-spaces"), "false");
	    if (tmp != null && tmp.equals("true")) {
		ignoreSpaceMode = true;
	    }
	}

	// now the easy part (for us!): use the input and make a diff
	try {

	    // Compute diff. Get the Patch object. Patch is the container for computed deltas.
	    if (sideBySideMode) {
		// Oh, we ain't got a barrel of money
		// Maybe we're ragged and funny
		// But we'll travel along, singin' a song
		// Side by side
		//
		// The library wraps using <br> HTML elements, so we turn
		// that off and instead pass the max-width attribute
		// to the output, e.g. for XSLT.
		//
		DiffRowGenerator generator = DiffRowGenerator.create()
		    .ignoreWhiteSpaces(ignoreSpaceMode)
		    .columnWidth(Integer.MAX_VALUE) // do not wrap
		    .build();
		List<DiffRow> rows = generator.generateDiffRows(
			original, revised
			);
		// now put the text from original and revised back
		// into the result, with spaces intact
		resultPipe.write(
			rowsToXML(
			    rows,
			    runtime, maxWidth));
	    } else {
		// default is Unix-style diff
		Patch<String> patch = DiffUtils.diff(
		                        listCopyWithoutSpaces(original),
		                        listCopyWithoutSpaces(revised));

		resultPipe.write(patchToXML(patch, runtime));
	    }

	} catch (Exception e) {
	    throw new XProcException(e);
	}
    } // run

    public static List<String> fileToLines(String filename) {
	List<String> lines = new LinkedList<String>();
	String line = "";
	BufferedReader in = null;
	try {
	    in = new BufferedReader(new FileReader(filename));
	    while ((line = in.readLine()) != null) {
		lines.add(line);
	    }
	} catch (IOException e) {
	    e.printStackTrace();
	} finally {
	    if (in != null) {
		try {
		    in.close();
		} catch (IOException e) {
		    // ignore ... any errors should already have been
		    // reported via an IOException from the final flush.
		}
	    }
	}
	return lines;
    } // fileToLines



    public static void main(String[] args) {

    }

}
