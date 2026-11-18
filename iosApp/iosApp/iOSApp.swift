import SwiftUI
import Shared

@main
struct iOSApp: App {
    var body: some Scene {
        WindowGroup {
            ContentView()
                // Forward the sign-in URL registered in Info.plist to MainViewController.kt.
                .onOpenURL { url in MainViewControllerKt.handleAuthUrl(url: url) }
        }
    }
}