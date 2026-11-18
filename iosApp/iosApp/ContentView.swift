import UIKit
import SwiftUI
import Shared

struct ComposeView: UIViewControllerRepresentable {
    // Ask MainViewController.kt for the shared Compose app and embed it in SwiftUI.
    func makeUIViewController(context: Self.Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Self.Context) {}
}

struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea()
    }
}