# Contributing
Ce fichier définit les conventions Git de l'équipe : nommage des branches, format des commits et contenu des Pull Requests.

## Branches

Toujours créer depuis `develop` :

- `feature/INV-12-description` — nouvelle fonctionnalité
- `fix/INV-45-description` — correction de bug
- `refactor/INV-30-description` — restructuration

## Commits

Format : Conventional Commits + référence Jira
```
feat(auth): implement login endpoint [INV-12]
fix(stock): prevent negative quantity on OUT [INV-34]
test(building): add integration tests [INV-20]
refactor(user): extract DTO mapper [INV-30]
docs: update README [INV-5]
chore: add Lombok dependency [INV-8]
```

## Pull Requests

Chaque PR vers `develop` doit contenir :

- Lien vers le ticket Jira
- Description du contexte
- Ce qui a été fait
- Comment tester