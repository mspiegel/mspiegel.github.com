<?xml version="1.0" encoding="utf-8"?>
<!-- *********************************************************************
     * specifyBindings.xsl
     * This is a stylesheet that takes a coercible simulation document and
     * allows you to specify alternate bindings for any and all of the
     * flexible points in the simulation.  Any unspecified flexible points
     * take on their default values.
     * Author: Joe Carnahan
     *
     * Contributors: Michael Spiegel (MathML) 
     ********************************************************************* -->

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                version="1.0">
  <xsl:import
    href="http://www.w3.org/Math/XSL/mathml.xsl" />
  <xsl:import 
    href="http://www.cs.virginia.edu/~jcc5t/research/flex_ml/assignments.xsl" />
  <xsl:import 
    href="http://www.cs.virginia.edu/~jcc5t/research/flex_ml/normalize.xsl" />
  <xsl:output method="text"/>


  <!-- This parameter gives the names and new values for each of the
       flexible points listed, where values are given as id=value strings
       separated by whitespace. -->
  <xsl:param name="bindings" />
  <xsl:variable 
    name="normalizedBindings">
    <xsl:call-template name="normalizeSpacing">
      <xsl:with-param name="string" select="$bindings" />
    </xsl:call-template>
  </xsl:variable>


  <!-- Main program template -->
  <xsl:template match="program">
    <xsl:variable name="s"><xsl:apply-templates /></xsl:variable>
    <!-- Trick to trim off leading whitespace, 
	 taken from http://skew.org/xml/stylesheets/trim/ -->
    <xsl:variable name="s-no-ws" select="translate($s,' &#9;&#10;&#13;','')"/>
    <xsl:variable name="s-first-non-ws" select="substring($s-no-ws,1,1)"/>
    <xsl:variable name="s-no-leading-ws" 
        select="concat($s-first-non-ws,substring-after($s,$s-first-non-ws))"/>
    <xsl:value-of select="$s-no-leading-ws"/>
  </xsl:template>


  <!-- Prevent attribute values from being printed out. -->
  <xsl:template match="@*" />

  <!-- Don't do anything with the header, skip to the content. -->
  <xsl:template match="programHeader" />


  <!-- For algorithmic flexible points, check if this alternative is
       included in the list of flexible points that have specified
       values.  If so, select the alternative(s) that has an ID matching
       one of the ones in the list of specified values. -->
  <xsl:template match="algorithmicFlexiblePoint">

    <!-- Get the name of this flexible point into a format where
         we can compare it with the given list of bindings. -->
    <xsl:variable name="thisBinding">
      <xsl:call-template name="bindingForm">
        <xsl:with-param name="string" select="@id" />
      </xsl:call-template>
    </xsl:variable>
    
    <xsl:choose>

      <xsl:when test="contains($normalizedBindings, $thisBinding)">        
        <!-- Extract the assigned value -->
        <xsl:variable
          name="afterID" 
          select="substring-after($normalizedBindings, $thisBinding)" />
        <xsl:variable
          name="afterIDbeforeSpace"
          select="substring-before($afterID,' ')" />
        <!-- Use the alternative indicated by the assigned value -->
        <xsl:value-of select="alternative[@id=$afterIDbeforeSpace]" />
      </xsl:when>
      
      <xsl:otherwise>
        <xsl:value-of select="alternative[@id=../@default]" />
      </xsl:otherwise>

    </xsl:choose>
  </xsl:template>


  <!-- For numeric flexible points, print out an initialization statement
       appropriate for the programming language used in this program. -->

  <xsl:template match="integerFlexiblePoint">

    <!-- Get the name of this flexible point into a format where
         we can compare it with the given list of bindings. -->
    <xsl:variable name="thisBinding">
      <xsl:call-template name="bindingForm">
        <xsl:with-param name="string" select="@id" />
      </xsl:call-template>
    </xsl:variable>

    <!-- The value to be assigned is either the default or the given
         value, if any. -->
    <xsl:choose>

      <xsl:when test="contains($normalizedBindings, $thisBinding)">
        <!-- Extract the given value -->
        <xsl:variable 
          name="afterID" 
          select="substring-after($normalizedBindings, $thisBinding)" />
        <xsl:variable
          name="afterIDbeforeSpace"
          select="substring-before($afterID,' ')" />
        <!-- Write an assignment statement using this given value -->
        <xsl:call-template name="assignInteger">
          <xsl:with-param name="value" select="$afterIDbeforeSpace" />
        </xsl:call-template>

      </xsl:when>
      <xsl:otherwise>
        <xsl:call-template name="assignInteger" />
      </xsl:otherwise>

    </xsl:choose>
  </xsl:template>

  <xsl:template match="numericFlexiblePoint">

      <!-- Get the name of this flexible point into a format where
           we can compare it with the given list of bindings. -->
      <xsl:variable name="thisBinding">
        <xsl:call-template name="bindingForm">
          <xsl:with-param name="string" select="@id" />
        </xsl:call-template>
      </xsl:variable>

    <!-- The value to be assigned is either the default or the given
         value, if any. -->
    <xsl:choose>

      <xsl:when test="contains($normalizedBindings, $thisBinding)">
        <!-- Extract the given value -->
        <xsl:variable 
          name="afterID" 
          select="substring-after($normalizedBindings, $thisBinding)" />
        <xsl:variable
          name="afterIDbeforeSpace"
          select="substring-before($afterID,' ')" />
        <!-- Write an assignment statement using the given value -->
        <xsl:call-template name="assignNumeric">
          <xsl:with-param name="value" select="$afterIDbeforeSpace" />
        </xsl:call-template>
      </xsl:when>

      <xsl:otherwise>
        <xsl:call-template name="assignNumeric" />
      </xsl:otherwise>

    </xsl:choose>
  </xsl:template>

  <xsl:template match="booleanFlexiblePoint">

    <!-- Get the name of this flexible point into a format where
         we can compare it with the given list of bindings. -->
    <xsl:variable name="thisBinding">
      <xsl:call-template name="bindingForm">
        <xsl:with-param name="string" select="@id" />
      </xsl:call-template>
    </xsl:variable>

    <!-- The value to be assigned is either the default or the given
         value, if any. -->
    <xsl:choose>

      <xsl:when test="contains($normalizedBindings, $thisBinding)">
        <!-- Extract the given value -->
        <xsl:variable 
          name="afterID" 
          select="substring-after($normalizedBindings, $thisBinding)" />
        <xsl:variable
          name="afterIDbeforeSpace"
          select="substring-before($afterID,' ')" />
        <!-- Write an assignment statement using the given value -->
        <xsl:call-template name="assignBoolean">
          <xsl:with-param name="value" select="$afterIDbeforeSpace" />
        </xsl:call-template>
      </xsl:when>

      <xsl:otherwise>
        <xsl:call-template name="assignBoolean" />
      </xsl:otherwise>

    </xsl:choose>
  </xsl:template>


</xsl:stylesheet>
