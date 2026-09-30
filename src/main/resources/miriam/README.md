# MIRIAM registry
Offline copy of the MIRIAM registry of identifiers.org, the JSON of the resolution API
https://registry.api.identifiers.org/resolutionApi/getResolverDataset

`MiriamRegistry` starts with this copy and replaces it with the current registry from that
URL in the background once the download succeeds. To update the copy, download the URL into
`MiriamRegistry.json`:

```bash
curl -sSf -o src/main/resources/miriam/MiriamRegistry.json \
  https://registry.api.identifiers.org/resolutionApi/getResolverDataset
```
