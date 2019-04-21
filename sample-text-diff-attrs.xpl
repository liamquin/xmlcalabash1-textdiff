<?xml version="1.0"?>
<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
    version="1.0"
    name="main"
    xmlns:dc="http://www.delightfulcomputing.com/ns/"
    xmlns:cx="http://xmlcalabash.com/ns/extensions">

    <p:output port="result"/>

    <p:import href="lib/textdiff.xpl" />

    <dc:textdiff original-text="Simon&#xa;Andrew&#xa;Nigel&#xa;Amanda&#xa;Theresa&#xa;">
	<p:input port="original">
	    <p:inline><p:data>doc1.txt</p:data></p:inline>
	</p:input>
	<p:input port="revised">
	    <p:inline><p:data>Liam
Simon
Andrew
David
Julie</p:data></p:inline>
	</p:input>
    </dc:textdiff>
</p:declare-step>
