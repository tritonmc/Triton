package com.rexcantor64.triton.spigot.banners;

import org.bukkit.block.banner.PatternType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PatternsTest {

    @Test
    public void testToPatternType() {
        // Test converting all patterns to their respective PatternType
        // This helps keep the conversion table up-to-date, since it would fail if the PatternType enum changes

        assertEquals(Patterns.BASE.toPatternType(), PatternType.BASE);
        assertEquals(Patterns.BL.toPatternType(), PatternType.SQUARE_BOTTOM_LEFT);
        assertEquals(Patterns.BO.toPatternType(), PatternType.BORDER);
        assertEquals(Patterns.BR.toPatternType(), PatternType.SQUARE_BOTTOM_RIGHT);
        assertEquals(Patterns.BRI.toPatternType(), PatternType.BRICKS);
        assertEquals(Patterns.BS.toPatternType(), PatternType.STRIPE_BOTTOM);
        assertEquals(Patterns.BT.toPatternType(), PatternType.TRIANGLE_BOTTOM);
        assertEquals(Patterns.BTS.toPatternType(), PatternType.TRIANGLES_BOTTOM);
        assertEquals(Patterns.CBO.toPatternType(), PatternType.CURLY_BORDER);
        assertEquals(Patterns.CR.toPatternType(), PatternType.CROSS);
        assertEquals(Patterns.CRE.toPatternType(), PatternType.CREEPER);
        assertEquals(Patterns.CS.toPatternType(), PatternType.STRIPE_CENTER);
        assertEquals(Patterns.DLS.toPatternType(), PatternType.STRIPE_DOWNLEFT);
        assertEquals(Patterns.DRS.toPatternType(), PatternType.STRIPE_DOWNRIGHT);
        assertEquals(Patterns.FLO.toPatternType(), PatternType.FLOWER);
        assertEquals(Patterns.GRA.toPatternType(), PatternType.GRADIENT);
        assertEquals(Patterns.HH.toPatternType(), PatternType.HALF_HORIZONTAL);
        assertEquals(Patterns.LD.toPatternType(), PatternType.DIAGONAL_LEFT);
        assertEquals(Patterns.LS.toPatternType(), PatternType.STRIPE_LEFT);
        assertEquals(Patterns.MC.toPatternType(), PatternType.CIRCLE);
        assertEquals(Patterns.MOJ.toPatternType(), PatternType.MOJANG);
        assertEquals(Patterns.MR.toPatternType(), PatternType.RHOMBUS);
        assertEquals(Patterns.MS.toPatternType(), PatternType.STRIPE_MIDDLE);
        assertEquals(Patterns.RD.toPatternType(), PatternType.DIAGONAL_RIGHT);
        assertEquals(Patterns.RS.toPatternType(), PatternType.STRIPE_RIGHT);
        assertEquals(Patterns.SC.toPatternType(), PatternType.STRAIGHT_CROSS);
        assertEquals(Patterns.SKU.toPatternType(), PatternType.SKULL);
        assertEquals(Patterns.SS.toPatternType(), PatternType.SMALL_STRIPES);
        assertEquals(Patterns.TL.toPatternType(), PatternType.SQUARE_TOP_LEFT);
        assertEquals(Patterns.TR.toPatternType(), PatternType.SQUARE_TOP_RIGHT);
        assertEquals(Patterns.TS.toPatternType(), PatternType.STRIPE_TOP);
        assertEquals(Patterns.TT.toPatternType(), PatternType.TRIANGLE_TOP);
        assertEquals(Patterns.TTS.toPatternType(), PatternType.TRIANGLES_TOP);
        assertEquals(Patterns.VH.toPatternType(), PatternType.HALF_VERTICAL);
        assertEquals(Patterns.LUD.toPatternType(), PatternType.DIAGONAL_UP_LEFT);
        assertEquals(Patterns.RUD.toPatternType(), PatternType.DIAGONAL_UP_RIGHT);
        assertEquals(Patterns.GRU.toPatternType(), PatternType.GRADIENT_UP);
        assertEquals(Patterns.HHB.toPatternType(), PatternType.HALF_HORIZONTAL_BOTTOM);
        assertEquals(Patterns.VHR.toPatternType(), PatternType.HALF_VERTICAL_RIGHT);
    }
}
