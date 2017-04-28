/*
 * Copyright (C) 2017 Andrejs Mivreņiks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.mivrenik.chess.uci;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Scanner;

/**
 * Helper class that handles communication with chess engine through UCI (Universal Chess Interface) protocol.
 */
public class ChessUci {

    /**
     * Stream to chess engine.
     */
    protected OutputStream mOutput;

    /**
     * Stream from chess engine.
     */
    protected BufferedReader mInput;

    /**
     * Engine name.
     */
    private String mEngineName;

    /**
     * Engine author name.
     */
    private String mEngineAuthor;

    /**
     * Current ponder position.
     */
    private String mPonder;

    /**
     * Constructor.
     *
     * @param output Output stream for sending commands to
     * @param input  Input stream for expecting response from
     */
    public ChessUci(OutputStream output, BufferedReader input) {
        this.mOutput = output;
        this.mInput = input;
    }

    /**
     * Send a command as is.
     * <p>
     *
     * @param command Command to send
     * @linkplain http://wbec-ridderkerk.nl/html/UCIProtocol.html
     */
    public void sendUciCommand(String command) {
        try {
            mOutput.write((command + "\n").getBytes());
            mOutput.flush();
        } catch (IOException ignored) {
        }
    }

    /**
     * Set position to start.
     */
    public void setPositionStart() {
        String command = "position startpos";
        sendUciCommand(command);
    }

    /**
     * Set position to start and perform moves.
     *
     * @param moves Moves to perform separated by space
     */
    public void setPositionStart(String moves) {
        String command = "position startpos moves " + moves;
        sendUciCommand(command);
    }

    /**
     * Set position to specified FEN.
     *
     * @param fen FEN (Forsyth–Edwards Notation) that represents chess position
     */
    public void setPositionFen(String fen) {
        String command = "position fen " + fen;
        sendUciCommand(command);
    }

    /**
     * Set position to specified FEN and perform moves.
     *
     * @param fen   FEN (Forsyth–Edwards Notation) that represents chess position
     * @param moves Moves to perform separated by space
     */
    public void setPositionFen(String fen, String moves) {
        String command = "position fen " + fen + " moves " + moves;
        sendUciCommand(command);
    }

    /**
     * Stop the engine.
     */
    public void stop() {
        sendUciCommand("quit");

        // Close streams
        try {
            mInput.close();
            mOutput.close();
        } catch (IOException ignored) {
        }
    }

    /**
     * Check if engine UCI is ready.
     *
     * @return Is engine ready
     */
    public boolean isReady() {
        sendUciCommand("isready");

        // Wait for response
        try {
            String line;
            while ((line = mInput.readLine()) != null) {
                if (line.compareToIgnoreCase("readyok") == 0) {
                    return true;
                }
            }
        } catch (IOException ignored) {
        }

        return false;
    }

    /**
     * Inform engine that user has played the expected move.
     */
    public void sendPonderHit() {
        sendUciCommand("ponderhit");
    }

    /**
     * Inform engine that next search will be from a different game.
     */
    public void sendNewGame() {
        sendUciCommand("ucinewgame");
    }

    /**
     * Tell the engine to use UCI and check status.
     *
     * @return UCI mode is ok
     */
    public boolean isUciOk() {
        sendUciCommand("uci");

        // Wait for response
        try {
            String line;
            while ((line = mInput.readLine()) != null) {
                Scanner scanner = new Scanner(line);
                scanner.useDelimiter(" ");

                if (scanner.findInLine("id name") != null) {
                    mEngineName = scanner.nextLine();
                }
                if (scanner.findInLine("id author") != null) {
                    mEngineAuthor = scanner.nextLine();
                }
                if (line.compareToIgnoreCase("uciok") == 0) {
                    return true;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Get engine name.
     *
     * @return Engine name
     */
    public String getEngineName() {
        return mEngineName != null ? mEngineName : "Unknown";
    }

    /**
     * Get engine author name.
     *
     * @return Engine author name
     */
    public String getEngineAuthor() {
        return mEngineAuthor != null ? mEngineAuthor : "Unknown";
    }

    /**
     * Get current ponder (expected by engine) move.
     *
     * @return Ponder move
     */
    public String getPonder() {
        return mPonder;
    }

    /**
     * Get best move from current position. It also persists ponder move that can be accessed using {@link #getPonder}.
     *
     * @return Best move
     */
    public String getBestMove() {
        String line;
        String bestMove;
        try {
            while ((line = mInput.readLine()) != null) {
                if (line.startsWith("bestmove")) {
                    Scanner scanner = new Scanner(line);
                    scanner.useDelimiter(" ");
                    scanner.next();
                    bestMove = scanner.next();
                    mPonder = scanner.hasNext() ? scanner.next() : null;

                    return bestMove;
                }
            }
        } catch (IOException ignored) {
        }

        return null;
    }
}

