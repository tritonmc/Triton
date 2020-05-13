package com.rexcantor64.triton.language.item;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.HashMap;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class LanguageSign extends LanguageItem {

    private List<SignLocation> locations;
    private HashMap<String, String[]> languages;

    @Override
    public LanguageItemType getType() {
        return LanguageItemType.SIGN;
    }


    public boolean hasLocation(com.rexcantor64.triton.api.language.SignLocation loc) {
        return hasLocation((SignLocation) loc, false);
    }

    public boolean hasLocation(SignLocation loc, boolean checkServer) {
        if (loc != null)
            for (SignLocation l : locations)
                if (checkServer ? loc.equalsWithServer(l) : loc.equals(l)) return true;
        return false;
    }

    public String[] getLines(String languageName) {
        return languages.get(languageName);
    }

}
