/*
 * Copyright © 2024 Ivan Akulinchev <ivan.akulinchev@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package me.testcase.ognarviewer.opengl;

import android.opengl.GLES20;

public class Shader {
    private final int mHandle;

    public Shader(String vertexSourceCode, String fragmentSourceCode) {
        final int vertexShaderHandle = loadShader(GLES20.GL_VERTEX_SHADER, vertexSourceCode);
        final int fragmentShaderHandle = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSourceCode);

        mHandle = GLES20.glCreateProgram();
        GLES20.glAttachShader(mHandle, vertexShaderHandle);
        GLES20.glAttachShader(mHandle, fragmentShaderHandle);
        GLES20.glLinkProgram(mHandle);

        // Not needed once compiled.
        GLES20.glDeleteShader(vertexShaderHandle);
        GLES20.glDeleteShader(fragmentShaderHandle);
    }

    public void use() {
        GLES20.glUseProgram(mHandle);
    }

    public int getAttributeLocation(String name) {
        final int handle = GLES20.glGetAttribLocation(mHandle, name);
        assert handle != -1;
        return handle;
    }

    public int getUniformLocation(String name) {
        final int handle = GLES20.glGetUniformLocation(mHandle, name);
        assert handle != -1;
        return handle;
    }

    private static int loadShader(int type, String sourceCode) {
        final int handle = GLES20.glCreateShader(type);
        GLES20.glShaderSource(handle, sourceCode);
        GLES20.glCompileShader(handle);
        return handle;
    }
}
