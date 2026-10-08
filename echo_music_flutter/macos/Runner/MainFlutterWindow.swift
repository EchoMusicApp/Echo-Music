import Cocoa
import FlutterMacOS

class MainFlutterWindow: NSWindow {
  override func awakeFromNib() {
    let flutterViewController = FlutterViewController()
    self.contentViewController = flutterViewController

    // The UI is the phone layout, so open at a comfortable size and stop the
    // window from shrinking below what the floating nav bar and player need.
    self.contentMinSize = NSSize(width: 380, height: 640)
    self.setContentSize(NSSize(width: 1100, height: 780))
    self.center()
    self.setFrameAutosaveName("EchoMusicMainWindow")

    RegisterGeneratedPlugins(registry: flutterViewController)

    super.awakeFromNib()
  }
}
