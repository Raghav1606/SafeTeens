package com.safeteens.child.database.local.typeConverters;

import androidx.room.TypeConverter;
import com.safeteens.child.enums.Sources;


public class SourceConverter {

    @TypeConverter
    public static Sources toSource(int value) {
        for(Sources source : Sources.values()) {
            if (source.ordinal() == value)
                return source;
        }
        throw new IllegalArgumentException("Could not recognize source value: " + value);
    }

    @TypeConverter
    public static Integer toInteger(Sources source) {
        return source.ordinal();
    }
}
