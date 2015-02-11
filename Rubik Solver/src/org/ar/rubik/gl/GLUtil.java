/**
 * Augmented Reality Rubik Cube Solver
 * 
 * Author: Steven P. Punte (aka Android Steve)
 * Date:   Nov 1st 2014
 * 
 * Project Description:
 *   Android application developed on a commercial Smart Phone which, when run on a pair 
 *   of Smart Glasses, guides a user through the process of solving a Rubik Cube.
 *   
 * File Description:
 *   OpenGL related utilities placed here to keep code elsewhere
 *   less cluttered and cleaner.
 * 
 * License:
 * 
 *  GPL
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.ar.rubik.gl;

import static android.opengl.GLES20.GL_COMPILE_STATUS;
import static android.opengl.GLES20.GL_VALIDATE_STATUS;
import static android.opengl.GLES20.glCompileShader;
import static android.opengl.GLES20.glCreateShader;
import static android.opengl.GLES20.glDeleteShader;
import static android.opengl.GLES20.glGetProgramInfoLog;
import static android.opengl.GLES20.glGetProgramiv;
import static android.opengl.GLES20.glGetShaderInfoLog;
import static android.opengl.GLES20.glGetShaderiv;
import static android.opengl.GLES20.glShaderSource;
import static android.opengl.GLES20.glValidateProgram;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.ar.rubik.Constants;

import android.content.Context;
import android.content.res.Resources;
import android.opengl.GLES20;
import android.util.Log;

/**
 * @author android.steve@testlens.com
 *
 */
public class GLUtil {

    /**
     * Reads in text from a resource file and returns a String containing the
     * text.
     */
    public static String readTextFileFromResource(Context context, int resourceId) {
        StringBuilder body = new StringBuilder();

        try {
            InputStream inputStream = context.getResources().openRawResource(resourceId);
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

            String nextLine;

            while ((nextLine = bufferedReader.readLine()) != null) {
                body.append(nextLine);
                body.append('\n');
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not open resource: " + resourceId, e);
        } catch (Resources.NotFoundException nfe) {
            throw new RuntimeException("Resource not found: " + resourceId, nfe);
        }

        return body.toString();
    }

    

    /**
     * Compiles a shader, returning the OpenGL object ID.
     * 
     * @param type
     * @param shaderCode
     * @return
     */
    public static int compileShader(int type, String shaderCode) {
        // Create a new shader object.
        final int shaderObjectId = glCreateShader(type);

        if (shaderObjectId == 0) {
            if (Constants.LOGGER) {
                Log.w(Constants.TAG_SHADER, "Could not create new shader.");
            }

            return 0;
        }

        // Pass in the shader source.
        glShaderSource(shaderObjectId, shaderCode);

        // Compile the shader.
        glCompileShader(shaderObjectId);

        // Get the compilation status.
        final int[] compileStatus = new int[1];
        glGetShaderiv(shaderObjectId, GL_COMPILE_STATUS, compileStatus, 0);

        if (Constants.LOGGER) {
            // Print the shader info log to the Android log output.
            Log.v(Constants.TAG_SHADER, "Results of compiling source:" + "\n" + shaderCode + "\n:"
                    + glGetShaderInfoLog(shaderObjectId));
        }

        // Verify the compile status.
        if (compileStatus[0] == 0) {
            // If it failed, delete the shader object.
            glDeleteShader(shaderObjectId);

            if (Constants.LOGGER) {
                Log.w(Constants.TAG_SHADER, "Compilation of shader failed.");
            }

            return 0;
        }

        // Return the shader object ID.
        return shaderObjectId;
    }
    
    
    /**
     * Validates an OpenGL program. Should only be called when developing the
     * application.
     */
    public static boolean validateProgram(int programObjectId) {
        glValidateProgram(programObjectId);
        final int[] validateStatus = new int[1];
        glGetProgramiv(programObjectId, GL_VALIDATE_STATUS, validateStatus, 0);
        Log.v(Constants.TAG_SHADER, "Results of validating program: " + validateStatus[0] + "\nLog:" + glGetProgramInfoLog(programObjectId));
        return validateStatus[0] != 0;
    }
    
    
    /**
    * Utility method for debugging OpenGL calls. Provide the name of the call
    * just after making it:
    *
    * <pre>
    * mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor");
    * MyGLRenderer.checkGlError("glGetUniformLocation");</pre>
    *
    * If the operation is not successful, the check throws an error.
    *
    * @param glOperation - Name of the OpenGL call to check.
    */
    public static void checkGlError(String glOperation) {
        
        int error;
        
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            Log.e(Constants.TAG, glOperation + ": glError " + error);
            throw new RuntimeException(glOperation + ": glError " + error);
        }
    }

}
