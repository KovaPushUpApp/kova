import AVFoundation

/// Option "Compter à voix haute" : annonce le numéro de chaque répétition.
enum Voix {
    private static let synthese = AVSpeechSynthesizer()

    static func dire(_ nombre: Int) {
        let enonce = AVSpeechUtterance(string: String(nombre))
        enonce.voice = AVSpeechSynthesisVoice(language: Locale.current.identifier.replacingOccurrences(of: "_", with: "-"))
        synthese.stopSpeaking(at: .immediate)
        synthese.speak(enonce)
    }
}
