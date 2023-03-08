package net.phbwt.paperwork.data.dao

import org.junit.Assert.*


import org.junit.Test

class DocumentQueryBuilderTest {

    private fun getBuilder() = DocumentQueryBuilder()

    @Test
    fun prepareFtsQuery_empty() {
        val r = getBuilder().prepareFtsQuery("")
        assertEquals("", r)
    }

    @Test
    fun prepareFtsQuery_empty_with_quote_begin() {
        val r = getBuilder().prepareFtsQuery("\"")
        assertEquals("", r)
    }

    @Test
    fun prepareFtsQuery_empty_with_quote_begin_blank() {
        val r = getBuilder().prepareFtsQuery("\"  ")
        assertEquals("", r)
    }

    @Test
    fun prepareFtsQuery_quoted_empty() {
        val r = getBuilder().prepareFtsQuery("\"\"")
        assertEquals("", r)
    }

    @Test
    fun prepareFtsQuery_quoted_blank() {
        val r = getBuilder().prepareFtsQuery("\"    \"")
        assertEquals("", r)
    }

    @Test
    fun prepareFtsQuery_lot_of_blank() {
        val r = getBuilder().prepareFtsQuery("  \"\" \"  \"\"\"\"     \"   \"  ")
        assertEquals("", r)
    }

    @Test
    fun prepareFtsQuery_single_as_prefix() {
        // typing probably not finished, the last word is used as a prefix
        val r = getBuilder().prepareFtsQuery("sampl")
        assertEquals("\"sampl*\"", r)
    }

    @Test
    fun prepareFtsQuery_single_not_prefixed() {
        // ending with a space, so the last word is not implicitly considered as a prefix
        val r = getBuilder().prepareFtsQuery("sample ")
        assertEquals("\"sample\"", r)
    }

    @Test
    fun prepareFtsQuery_multiple_as_prefix() {
        // typing probably not finished, the last word is used as a prefix
        val r = getBuilder().prepareFtsQuery("some sampl")
        assertEquals("\"some\" \"sampl*\"", r)
    }

    @Test
    fun prepareFtsQuery_multiple_not_prefixed() {
        // ending with a space, so the last word is not implicitly considered as a prefix
        val r = getBuilder().prepareFtsQuery("some sample ")
        assertEquals("\"some\" \"sample\"", r)
    }

    @Test
    fun prepareFtsQuery_quoted_simple() {
        val r = getBuilder().prepareFtsQuery("\"some sample\"")
        assertEquals("\"some sample\"", r)
    }

    @Test
    fun prepareFtsQuery_quoted_trailing_return() {
        // fixed bug
        val r = getBuilder().prepareFtsQuery("\"sample\"\n")
        assertEquals("\"sample\"", r)
    }

    @Test
    fun prepareFtsQuery_quoted_containing_return() {
        // fixed bug
        val r = getBuilder().prepareFtsQuery("\"the\nsample\"\n")
        assertEquals("\"the sample\"", r)
    }

    @Test
    fun prepareFtsQuery_quoted_with_spacing() {
        val r = getBuilder().prepareFtsQuery("  \"some   sample\"    ")
        assertEquals("\"some sample\"", r)
    }

    @Test
    fun prepareFtsQuery_quoted_prefixed_star() {
        val r = getBuilder().prepareFtsQuery("\"some sample*\"")
        assertEquals("\"some sample*\"", r)
    }

    @Test
    fun prepareFtsQuery_quoted_prefixed_space() {
        val r = getBuilder().prepareFtsQuery("\"some sample \"")
        assertEquals("\"some sample \"", r)
    }
    @Test
    fun prepareFtsQuery_quoted_unfinished() {
        // quote not closed, last word mays be a prefix
        val r = getBuilder().prepareFtsQuery("\"some sampl")
        assertEquals("\"some sampl*\"", r)
    }

    @Test
    fun prepareFtsQuery_quoted_unfinished_without_prefix() {
        // quote not closed, last word is complete
        val r = getBuilder().prepareFtsQuery("\"some sample ")
        assertEquals("\"some sample \"", r)
    }

    @Test
    fun prepareFtsQuery_quoted_unfinished_already_prefix() {
        // quote not closed, last word is explicitly a prefix
        val r = getBuilder().prepareFtsQuery("\"some sampl*")
        assertEquals("\"some sampl*\"", r)
    }

    @Test
    fun prepareFtsQuery_2_complete() {
        val r = getBuilder().prepareFtsQuery("first val1 \"second val2\"")
        assertEquals("\"first\" \"val1\" \"second val2\"", r)
    }

    @Test
    fun prepareFtsQuery_2_no_space() {
        val r = getBuilder().prepareFtsQuery("first val1\"second val2\"")
        assertEquals("\"first\" \"val1\" \"second val2\"", r)
    }

    @Test
    fun prepareFtsQuery_2_quote_begin() {
        val r = getBuilder().prepareFtsQuery("first value \"second val")
        assertEquals("\"first\" \"value\" \"second val*\"", r)
    }

    @Test
    fun prepareFtsQuery_multiple_alterned() {
        val r = getBuilder().prepareFtsQuery("\"zeroth val0\" first val1 unquoted \"second val2 quoted\" third val3 \"forth val4\"")
        assertEquals("\"zeroth val0\" \"first\" \"val1\" \"unquoted\" \"second val2 quoted\" \"third\" \"val3\" \"forth val4\"", r)
    }

    @Test
    fun prepareFtsQuery_multiple_quoted_various_spacing() {
        // quoted parts separated by 0, 1 or more spaces
        val r = getBuilder().prepareFtsQuery("\"first val1\"\"second val2 quoted\" \"third val3\"   \"forth val4\"   ")
        assertEquals("\"first val1\" \"second val2 quoted\" \"third val3\" \"forth val4\"", r)
    }


}