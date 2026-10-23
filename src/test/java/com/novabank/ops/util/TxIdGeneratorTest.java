package com.novabank.ops.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

public class TxIdGeneratorTest {

    private final TxIdGenerator generator = new TxIdGenerator();

    @Test
    public void 형식은_접두어2_영숫자6_체크2() {
        String id = generator.generate();
        assertEquals(10, id.length());
        assertTrue(id.startsWith("NB"));
        assertTrue(id.matches("^[A-Z0-9]{10}$"));
    }

    @Test
    public void 체크_문자로_오타를_잡는다() {
        String id = generator.generate();
        assertTrue(generator.isValid(id));
        String typo = id.substring(0, 2) + (id.charAt(2) == 'A' ? 'B' : 'A') + id.substring(3);
        assertFalse(generator.isValid(typo));
        assertFalse(generator.isValid(null));
        assertFalse(generator.isValid("NB1"));
    }

    @Test
    public void 천_번_생성해도_중복이_없다() {
        Set<String> seen = new HashSet<String>();
        for (int i = 0; i < 1000; i++) {
            seen.add(generator.generate());
        }
        assertEquals(1000, seen.size());
    }

    @Test
    public void 체크섬은_결정적이다() {
        assertEquals(TxIdGenerator.checksum("NBAB12CD"), TxIdGenerator.checksum("NBAB12CD"));
    }
}
