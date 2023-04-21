package com.text2sql4j.api.models.movies

/**
 * Taken from the dataset:
 * R:roletypes (always three characters).
 * Later assignments at end,  so that `R:Und' can be replaced by
 * <TABLE> (extra space in `tr/td' to disable count-matches)
 * <tr> <td> \Adv<td> adversary<td> |
 * <tr> <td> \Agn<td> agent<td> |
 * <tr> <td> \Ani<td> animal<td> |
 * <tr> <td> \Bit<td> bit role<td> |
 * <tr> <td> \Cam<td> cameo role<td> |
 * <tr> <td> \Cro<td> crook<td> |
 * <tr> <td> \Grp<td> group or band<td> |
 * <tr> <td> \Her<td> hero<td> |
 * <tr> <td> \Inn<td> innocent<td> |
 * <tr> <td> \Lov<td> love interest<td> |
 * <tr> <td> \Sav<td> savior<td> |
 * <tr> <td> \Sci<td> scientist<td> |
 * <tr> <td> \Sdk<td> sidekick<td> |
 * <tr> <td> \Sus<td> suspect<td> |
 * <tr> <td> \Rul<td> ruler<td> |
 * <tr> <td> \Psy<td> psychopath<td> |
 * <tr> <td> \Und<td> undetermined<td> |
 * <tr> <td> \Vmp<td> vamp<td> |
 * <tr> <td> \Vic<td> victim<td> |
 * <tr> <td> \Vil<td> villain}<td> |
 * <tr> <td> \Voi<td> voice only, narrator<td> |
 * <tr> <td> \Wmp<td> wimp<td> |
 * </TABLE>
 */
enum class ActorRole(val code: String) {
    UNDETERMINED("\\Und"),
    ADVERSARY("\\Adv"),
    AGENT("\\Agn"),
    ANIMAL("\\Ani"),
    BIT("\\Bit"),
    CAMEO("\\Cam"),
    CROOK("\\Cro"),
    GROUP("\\Grp"),
    HERO("\\Her"),
    INNOCENT("\\Inn"),
    LOVE_INTEREST("\\Lov"),
    SAVIOR("\\Sav"),
    SCIENTIST("\\Sci"),
    SIDEKICK("\\Sdk"),
    SUSPECT("\\Sus"),
    RULER("\\Rul"),
    PSYCHOPATH("\\Psy"),
    VAMPIRE("\\Vmp"),
    VICTIM("\\Vic"),
    VILLIAN("\\Vil"),
    NARRATOR("\\Voi"),
    WIMP("\\Wmp");

    companion object {
        fun fromCode(code: String): ActorRole {
            return values().find { role -> role.code == code }
                ?: UNDETERMINED
        }
    }
}