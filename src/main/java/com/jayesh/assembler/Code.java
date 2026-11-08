package com.jayesh.assembler;

import java.io.ByteArrayOutputStream;

public class Code {
    private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();

    public void emitByte(long value) { bytes.write((int)value & 0xFF); }

    public void emitLE(long value, int size) {
        for (int i = 0; i < size; i++) emitByte(value >>> (8 * i));
    }

    public byte[] toByteArray() { return bytes.toByteArray(); }
    public int size() { return bytes.size(); }

    public int byteAt(int index) { return bytes.toByteArray()[index] & 0xFF; }

    public void patchLE(int offset, long value, int size) {
        byte[] b = bytes.toByteArray();
        for (int i = 0; i < size; i++) b[offset + i] = (byte)(value >>> (8 * i));
        bytes.reset();
        bytes.writeBytes(b);
    }

    public String hex() {
        StringBuilder s = new StringBuilder();
        for (byte b : bytes.toByteArray()) s.append(String.format("%02X", b & 0xFF));
        return s.toString();
    }
}
