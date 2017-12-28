package com.hierynomus.sshj.sftp

import com.hierynomus.sshj.IntegrationBaseSpec
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.sftp.OpenMode
import net.schmizz.sshj.sftp.RemoteFile
import net.schmizz.sshj.sftp.SFTPClient

import java.nio.charset.StandardCharsets

import static org.codehaus.groovy.runtime.IOGroovyMethods.withCloseable

class FileWriteSpec extends IntegrationBaseSpec {

    def "should append to file (GH issue #390)"() {
        given:
        SSHClient client = getConnectedClient()
        client.authPublickey("sshj", "src/test/resources/id_rsa")
        SFTPClient sftp = client.newSFTPClient()
        def file = "/home/sshj/test.txt"
        def initialText = "This is the initial text.\n".getBytes(StandardCharsets.UTF_16)
        def appendText = "And here's the appended text.\n".getBytes(StandardCharsets.UTF_16)

        when:
        withCloseable(sftp.open(file, EnumSet.of(OpenMode.WRITE, OpenMode.CREAT))) { RemoteFile initial ->
            initial.write(0, initialText, 0, initialText.length)
        }

        then:
        withCloseable(sftp.open(file, EnumSet.of(OpenMode.READ))) { RemoteFile read ->
            def bytes = new byte[initialText.length]
            read.read(0, bytes, 0, bytes.length)
            bytes == initialText
        }

        when:
        withCloseable(sftp.open(file, EnumSet.of(OpenMode.WRITE, OpenMode.APPEND))) { RemoteFile append ->
            append.write(0, appendText, 0, appendText.length)
        }

        then:
        withCloseable(sftp.open(file, EnumSet.of(OpenMode.READ))) { RemoteFile read ->
            def bytes = new byte[initialText.length + appendText.length]
            read.read(0, bytes, 0, bytes.length)
            Arrays.copyOfRange(bytes, 0, initialText.length) == initialText
            Arrays.copyOfRange(bytes, initialText.length, initialText.length + appendText.length) == appendText
        }

        cleanup:
        sftp.close()
        client.close()
    }
}
