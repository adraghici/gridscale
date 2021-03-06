### SSH server
To access a storage through **SSH**:

    val sshStorage = new SSHStorage with SSHUserPasswordAuthentication {
      def host: String = "server.domain"
      def user = "username"
      def password = "password"
    }
    
    sshStorage.list(".").foreach(println)
    sshStorage.makeDir("/tmp/testdir")
    
    val os = sshStorage.openOutputStream("/tmp/testdir/test.txt")
    try os.write("Cool gridscale".toCharArray.map(_.toByte))
    finally os.close
    
    val b = Array.ofDim[Byte](100)
    val is = sshStorage.openInputStream("/tmp/testdir/test.txt")
    try is.read(b)
    finally is.close
    
    println(new String(b.map(_.toChar)))
    
    sshStorage.rmDir("/tmp/testdir")

To run a process on a remote server through **SSH**:

    val sshJS = new SSHJobService with SSHUserPasswordAuthentication {
      def host: String = "server.domain"
      def user = "user"
      def password = "password"
    }
    
    val jobDesc = new SSHJobDescription {
      def executable = "/bin/touch"
      def arguments = "/tmp/test.ssh"
      def workDirectory: String = "/tmp/"
    }
    
    val j = sshJS.submit(jobDesc)
    val s = untilFinished{Thread.sleep(5000); val s = sshJS.state(j); println(s); s}
    sshJS.purge(j)

