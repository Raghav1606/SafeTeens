package com.safeteens.child.database.local.typeConverters;

import androidx.room.TypeConverter;

import com.safeteens.child.enums.ChateeTypes;


public class ChateeTypeConverter {

    @TypeConverter
    public static ChateeTypes toSource(int value) {
        for(ChateeTypes type : ChateeTypes.values()) {
            if (type.ordinal() == value)
                return type;
        }
        throw new IllegalArgumentException("Could not recognize source value: " + value);
    }

    @TypeConverter
    public static Integer toInteger(ChateeTypes type) {
        return type.ordinal();
    }
}
