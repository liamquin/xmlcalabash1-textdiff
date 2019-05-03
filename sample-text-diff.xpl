<?xml version="1.0"?>
<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
    version="1.0"
    name="main"
    xmlns:dc="http://www.delightfulcomputing.com/ns/"
    xmlns:cx="http://xmlcalabash.com/ns/extensions">

    <p:output port="result"/>

    <p:import href="lib/textdiff.xpl" />

    <!--* options to dc:textdiff:
        * mode: this can be default or side-by-side
	* other modes may be added in the future; the value
	* is passed through to the output as @diffmode
	*
	* max-width: this is an integer. It's normally used only for
	* side-by-side diff, but in any case it's just passed through
	* to the output
	*
	* original-text and revised-text contain the data; 
	* They are also passed to the output as attributes.
	*
	* You can omit one of the two -text attributes; in this case
	* the setp will try to read the source port for the omitted value,
	* which should be an XML document consisting of a single outermost
	* element containing the text directly.
	*
	*-->
    <dc:textdiff
	diff-mode="default"
	max-width="78"
	ignore-spaces="true">

	<p:with-option name="original-text" select="unparsed-text('original.txt')" />
	<p:with-option name="revised-text" select="unparsed-text('revised.txt')" />
	<p:input port="source">
	    <p:inline><p:data>lines to be compared&#xa;could go here&#xa;with an element wrapper</p:data></p:inline>
	</p:input>
    </dc:textdiff>
</p:declare-step>
