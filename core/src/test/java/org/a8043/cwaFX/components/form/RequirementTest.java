package org.a8043.cwaFX.components.form;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequirementTest {
    @Test
    void textLengthRequirementUsesInclusiveBoundsAndAppropriateDefaultMessages() {
        TextLengthRequirement requirement = new TextLengthRequirement();
        requirement.setMinLength(2);
        requirement.setMaxLength(4);

        assertTrue(requirement.validate("ab").isValid());
        assertTrue(requirement.validate("abcd").isValid());

        ValidationResult tooShort = requirement.validate("a");
        assertFalse(tooShort.isValid());
        assertArrayEquals(new Object[]{2, 4}, tooShort.getArguments());
        assertTrue("form.validation.text-length".equals(tooShort.getMessageKey()));

        requirement.setMinLength(0);
        ValidationResult tooLong = requirement.validate("abcde");
        assertTrue("form.validation.text-length.max".equals(tooLong.getMessageKey()));

        requirement.setMinLength(2);
        requirement.setMaxLength(Integer.MAX_VALUE);
        assertTrue("form.validation.text-length.min".equals(requirement.validate("a").getMessageKey()));
    }

    @Test
    void textLengthRequirementRejectsInvalidInputsAndConfigurations() {
        TextLengthRequirement requirement = new TextLengthRequirement();

        assertThrows(IllegalArgumentException.class, () -> requirement.validate(1));
        requirement.setMinLength(-1);
        assertThrows(IllegalStateException.class, () -> requirement.validate("text"));

        requirement.setMinLength(4);
        requirement.setMaxLength(3);
        assertThrows(IllegalStateException.class, () -> requirement.validate("text"));

        assertTrue(requirement.supports(String.class));
        assertFalse(requirement.supports(Double.class));
    }

    @Test
    void numberRangeRequirementUsesInclusiveBoundsAndChoosesDefaultMessages() {
        NumberRangeRequirement requirement = new NumberRangeRequirement();
        requirement.setMin(1.5);
        requirement.setMax(3.5);

        assertTrue(requirement.validate(1.5).isValid());
        assertTrue(requirement.validate(3.5).isValid());

        ValidationResult outOfRange = requirement.validate(4);
        assertFalse(outOfRange.isValid());
        assertTrue("form.validation.number-range".equals(outOfRange.getMessageKey()));
        assertArrayEquals(new Object[]{1.5, 3.5}, outOfRange.getArguments());

        requirement.setMin(Double.NEGATIVE_INFINITY);
        assertTrue("form.validation.number-range.max".equals(requirement.validate(4).getMessageKey()));
        requirement.setMin(0);
        requirement.setMax(Double.POSITIVE_INFINITY);
        assertTrue("form.validation.number-range.min".equals(requirement.validate(-1).getMessageKey()));

        requirement.setMessageKey("custom.range");
        assertTrue("custom.range".equals(requirement.validate(-1).getMessageKey()));
    }

    @Test
    void numberRangeRequirementRejectsInvalidInputsAndConfigurations() {
        NumberRangeRequirement requirement = new NumberRangeRequirement();

        assertThrows(IllegalArgumentException.class, () -> requirement.validate("1"));
        requirement.setMin(Double.NaN);
        assertThrows(IllegalStateException.class, () -> requirement.validate(1));

        requirement.setMin(4);
        requirement.setMax(3);
        assertThrows(IllegalStateException.class, () -> requirement.validate(1));

        assertTrue(requirement.supports(Double.class));
        assertFalse(requirement.supports(String.class));
    }

    @Test
    void validationResultCopiesSuppliedArguments() {
        Object[] arguments = {"original"};
        ValidationResult result = ValidationResult.invalid("message", arguments);
        arguments[0] = "changed";

        assertArrayEquals(new Object[]{"original"}, result.getArguments());
    }
}
