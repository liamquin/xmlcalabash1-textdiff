<p:library xmlns:p="http://www.w3.org/ns/xproc"
	   xmlns:dc="http://www.delightfulcomputing.com/ns/"
           version="1.0">

<p:declare-step type="dc:textdiff">
   <p:output port="result"/>
   <p:input port="source" />
   <p:option name="original-text" />
   <p:option name="revised-text" />
   <p:option name="diff-mode" /> <!--* default or side-by-side *-->
   <p:option name="ignore-spaces" /> <!--* default or side-by-side *-->
   <p:option name="max-width" /> <!--* passed through to output *-->
</p:declare-step>
</p:library>
