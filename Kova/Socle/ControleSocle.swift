import SwiftUI

/// Écran de contrôle, pour vérifier un build sur l'iPhone : polices chargées,
/// couleurs, langue, version, extensions embarquées, verrouillage et achats.
/// Écran de chantier, à retirer avant la mise en ligne.
struct ControleSocle: View {
    @EnvironmentObject private var etat: EtatApp
    @EnvironmentObject private var abonnements: Abonnements
    let retour: () -> Void

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: Tokens.Espacements.e21) {
                BoutonRetour(action: retour)

                Text("socle.titre")
                    .font(.kovaTitre(Tokens.TaillesTexte.titreEcran))
                    .foregroundColor(Tokens.Couleurs.texte)

                bloc("socle.polices") {
                    ForEach(Police.toutes, id: \.self) { nom in
                        ligne(nom, valeur: Police.estChargee(nom) ? "socle.ok" : "socle.absente",
                              valide: Police.estChargee(nom))
                    }
                    Text("socle.exemple_titre")
                        .font(.kovaTitre(Tokens.TaillesTexte.titreCarte))
                        .foregroundColor(Tokens.Couleurs.texte)
                    Text("socle.exemple_texte")
                        .font(.kovaTexte())
                        .foregroundColor(Tokens.Couleurs.texteSecondaire)
                }

                bloc("socle.couleurs") {
                    HStack(spacing: Tokens.Espacements.e8) {
                        pastille(Tokens.Couleurs.accent)
                        pastille(Tokens.Couleurs.texte)
                        pastille(Tokens.Couleurs.texteSecondaire)
                        pastille(Tokens.Couleurs.ligne)
                        pastille(Tokens.Couleurs.carteSelectionnee)
                        pastille(Tokens.Couleurs.fond)
                    }
                }

                bloc("socle.chantier") {
                    HStack {
                        Text("socle.simulation")
                            .font(.kovaTexte())
                            .foregroundColor(Tokens.Couleurs.texte)
                        Spacer()
                        Interrupteur(actif: abonnements.simulation, libelle: L.t("socle.simulation")) {
                            abonnements.simulation.toggle()
                        }
                    }
                    BoutonSecondaire(L.t("socle.revoir_onboarding")) {
                        etat.onboardingTermine = false
                    }
                }

                bloc("socle.build") {
                    ligne("socle.verrouillage", valeur: Verrouillage.disponible ? "socle.oui" : "socle.non",
                          valide: Verrouillage.disponible)
                    ligne("socle.achats", valeur: abonnements.configure ? "socle.oui" : "socle.non",
                          valide: abonnements.configure)
                    ligne("socle.langue", valeur: "socle.langue_valeur", valide: true)
                    ligneTexte("socle.version", valeur: version)
                    ligneTexte("socle.extensions", valeur: extensions)
                    ligneTexte("socle.groupe", valeur: GroupePartage.identifiant ?? "-")
                }
            }
            .padding(.horizontal, Tokens.Ecran.margeCotes)
            .padding(.top, Tokens.Espacements.e8)
            .padding(.bottom, Tokens.Ecran.margeBas)
        }
        .background(Tokens.Couleurs.fond.ignoresSafeArea())
        .toolbar(.hidden, for: .navigationBar)
    }

    private var version: String {
        let infos = Bundle.main.infoDictionary
        let numero = infos?["CFBundleShortVersionString"] as? String ?? "?"
        let build = infos?["CFBundleVersion"] as? String ?? "?"
        return "\(numero) (\(build))"
    }

    /// Noms des extensions réellement embarquées dans ce build.
    private var extensions: String {
        guard let dossier = Bundle.main.builtInPlugInsURL,
              let contenu = try? FileManager.default.contentsOfDirectory(
                  at: dossier, includingPropertiesForKeys: nil
              ) else {
            return "0"
        }
        let noms = contenu
            .filter { $0.pathExtension == "appex" }
            .map { $0.deletingPathExtension().lastPathComponent }
            .sorted()
        return noms.isEmpty ? "0" : noms.joined(separator: ", ")
    }

    private func bloc<Contenu: View>(
        _ titre: LocalizedStringKey,
        @ViewBuilder contenu: () -> Contenu
    ) -> some View {
        VStack(alignment: .leading, spacing: Tokens.Espacements.e13) {
            Text(titre)
                .font(.kovaTexte(Tokens.TaillesTexte.legende, graisse: .demiGras, relatifA: .caption))
                .foregroundColor(Tokens.Couleurs.texteSecondaire)
            contenu()
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(Tokens.Espacements.e21)
        .background(Tokens.Couleurs.carte)
        .clipShape(RoundedRectangle(cornerRadius: Tokens.Arrondis.grand, style: .continuous))
    }

    private func ligne(_ libelle: String, valeur: LocalizedStringKey, valide: Bool) -> some View {
        HStack {
            Text(LocalizedStringKey(libelle))
                .font(.kovaTexte())
                .foregroundColor(Tokens.Couleurs.texte)
            Spacer()
            Text(valeur)
                .font(.kovaTexte(graisse: .demiGras))
                .foregroundColor(valide ? Tokens.Couleurs.accent : Tokens.Couleurs.texteSecondaire)
        }
    }

    private func ligneTexte(_ libelle: LocalizedStringKey, valeur: String) -> some View {
        HStack(alignment: .top) {
            Text(libelle)
                .font(.kovaTexte())
                .foregroundColor(Tokens.Couleurs.texte)
            Spacer()
            Text(verbatim: valeur)
                .font(.kovaTexte(Tokens.TaillesTexte.legende, relatifA: .caption))
                .foregroundColor(Tokens.Couleurs.texteSecondaire)
                .multilineTextAlignment(.trailing)
        }
    }

    private func pastille(_ couleur: Color) -> some View {
        RoundedRectangle(cornerRadius: Tokens.Arrondis.petit, style: .continuous)
            .fill(couleur)
            .frame(width: Tokens.Composants.VignetteApp.taille, height: Tokens.Composants.VignetteApp.taille)
    }
}
