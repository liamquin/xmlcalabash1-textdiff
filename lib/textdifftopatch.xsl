<?xml version="1.0" encoding="UTF-8" ?>
<xsl:stylesheet version="2.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:dc="http://www.delightfulcomputing.com/ns/"
  xmlns:xs="http://www.w3.org/2001/XMLSchema"
  exclude-result-prefixes="dc xs"
  >

  <!--* Convert XML from the TextDiff XProc step into
      * something like Unix diff output.
      *-->

  <xsl:output method="text"/>

  <xsl:template match="dc:textdiff">
    <xsl:apply-templates select="*"/>
  </xsl:template>

  <xsl:variable name="tags" as="element(dc:tag)*">
    <!--* the tag attribute is used in the middle column and must be one
        * Unicode character wide exactly:
        *-->
    <dc:tag for="insert" tag="&gt;" />
    <dc:tag for="delete" tag="&lt;" />
    <dc:tag for="change" tag="|" />
    <dc:tag for="equal" tag=" " />
    <dc:tag for="skip" tag="." />
  </xsl:variable>

  <!--* side by side diff is indicated by textdif diffmode="side-by-side" *-->
  <xsl:variable name="maxwidth" as="xs:integer?" select="/dc:textdiff/@maxwidth" />

  <xsl:template match="dc:row">
    <xsl:variable name="colwidth" as="xs:integer"
              select="if (/dc:textdiff/@maxwidth)
              then (xs:integer(/dc:textdiff/@maxwidth) - 3) idiv 2
              else 38" />
    <xsl:variable name="left"
              select="substring(dc:original, 1, $colwidth)" as="xs:string" />
    <xsl:variable name="leftwidth" select="string-length(dc:original)" />

    <xsl:variable name="right"
              select="substring(dc:revised, 1, $colwidth)" as="xs:string" />
    <xsl:variable name="rightwidth" select="string-length(dc:revised)" />

    <xsl:variable name="padding" as="xs:string"
              select="string-join(for $i in (1 to $colwidth) return ' ', ' ')"/>

    <!--* the left-hand line of text *-->

    <xsl:choose>
      <xsl:when test="$leftwidth eq $colwidth">
	<xsl:value-of select="$left" />
      </xsl:when>
      <xsl:when test="$leftwidth lt $colwidth">
	<!--* pad out with spaces if the left string wasn't long enough *-->
	<xsl:value-of select="$left" />
	<xsl:value-of select="substring($padding, 1, $colwidth - $leftwidth)" />
      </xsl:when>
      <xsl:otherwise>
	<!--* add an ellipsis to show truncation *-->
	<xsl:value-of select="substring($left, 1, $colwidth - 1)" />
	<xsl:value-of select=" '…' "/>
      </xsl:otherwise>
    </xsl:choose>

    <!--* the left gutter *-->
    <xsl:value-of select=" ' ' " />

    <!--* the tag *-->
    <xsl:value-of select="$tags[@for eq current()/@tag]/@tag" />

    <!--* the right gutter *-->
    <xsl:value-of select=" ' ' " />

    <!--* the right-hand line of text *-->
    <xsl:choose>
      <xsl:when test="$rightwidth le $colwidth">
	<!--* le here because we don't need to pad if the string is short *-->
	<xsl:value-of select="$right" />
      </xsl:when>
      <xsl:otherwise>
	<!--* add an ellipsis to show truncation *-->
	<xsl:value-of select="substring($right, 1, $colwidth - 1)" />
	<xsl:value-of select=" '…' "/>
      </xsl:otherwise>
    </xsl:choose>

    <!--* final newline *-->
    <xsl:value-of select=" '&#xa;' " />

  </xsl:template>

  <xsl:template match="dc:delta">
    <!--* Handle three possible types of change>
        * A change
        * an insertion
        * a deletion
        *
        * For insertion, the line number in the original file is reported
        * as the line before the change, e.g.,
        * 31a
        * to append after line 31.
        * For deletion, the line in the revised file is one smaller.
        *
        * Since the Java library numbers lines from zero, this means
        * we add one in the changed case.
        *-->
    <xsl:variable name="actions" as="element(dc:action)*">
      <dc:action when="insert" command="a" left="0" right="1" />
      <dc:action when="change" command="c" left="1" right="1" />
      <dc:action when="delete" command="d" left="1" right="0" />
    </xsl:variable>

    <xsl:variable name="action" as="element(dc:action)?"
      select="$actions[@when = current()/@type]" />

    <xsl:if test="not($action/@command)">
      <xsl:message terminate="yes">
        <xsl:value-of select="concat('textdifftopatch.xsl: unknown dc:delta type ', current()/@type, ' - expected insert, change or delete')" />
      </xsl:message>
    </xsl:if>

    <xsl:value-of select="dc:original/@position + $action/@left" />
    <xsl:if test="xs:integer(dc:original/@size) &gt; 1">
      <xsl:text>,</xsl:text>
      <xsl:value-of select="dc:original/@size + $action/@left" />
    </xsl:if>

    <xsl:value-of select="$action/@command" />

    <xsl:value-of select="dc:revised/@position + $action/@right"/>
    <xsl:if test="dc:revised/@size &gt; 1">
      <xsl:text>,</xsl:text>
      <xsl:value-of select="dc:revised/@size + $action/@right "/>
    </xsl:if>
    <xsl:text>&#xa;</xsl:text>

    <xsl:apply-templates select="dc:original"/>

    <xsl:if test="dc:original/@size &gt; 0 and dc:revised/@size &gt; 0">
      <xsl:text>---&#xa;</xsl:text> <!--* separator *-->
    </xsl:if>

    <xsl:apply-templates select="dc:revised"/>

  </xsl:template>

  <xsl:template match="dc:original">
    <xsl:for-each select="dc:chunk">
      <xsl:value-of select="concat('&lt; ', ., '&#xa;')"/>
    </xsl:for-each>
  </xsl:template>

  <xsl:template match="dc:revised">
    <xsl:for-each select="dc:chunk">
      <xsl:value-of select="concat('&gt; ', ., '&#xa;')"/>
    </xsl:for-each>
  </xsl:template>
</xsl:stylesheet>
