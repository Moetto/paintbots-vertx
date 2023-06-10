package paintbots.app

import java.io.File


fun makeVideo(workingDirectory: File, videoFileName: String) {
    val p = ProcessBuilder(
        "ffmpeg",
        "-framerate", "5",
        "-pattern_type", "glob",
        "-i", "*.png",
        "-c:v",
        "libx264",
        "-pix_fmt", "yuv420p",
        videoFileName
    )
    assert(workingDirectory.exists())
    assert(workingDirectory.isDirectory)

    p.directory(workingDirectory)
        .start()
}
