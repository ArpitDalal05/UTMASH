# UT Mesh Rebrand Notes

This checkout has been rebranded for the UT Mesh project.

- Application ID: `com.arpit.utmesh`
- Namespace: `com.arpit.utmesh`
- App label: `UT Mesh`
- Application class: `com.arpit.utmesh.UtMeshApp`
- Deep-link scheme: `utmesh://sso/...`
- Launcher/splash assets use the UT Mesh logo.

The upstream Crisis Connect source remains present in this checkout where it is required by
existing functionality. Upstream licensing and attribution should be retained when distributing
modified builds.

## Build prerequisites

The project still contains Firebase-backed features inherited from the upstream application.
A real `app/google-services.json` is required for builds/features that use Firebase. Do not put
placeholder credentials into a release build.

For the FF180 UT-to-UT offline requirement, the Bluetooth/mesh stack should be treated as the
primary local transport; Internet/Firebase features should be disabled or removed in a later
cleanup pass rather than silently being considered part of the offline protocol.
