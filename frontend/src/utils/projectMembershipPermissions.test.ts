import { describe, expect, it } from 'vitest'
import {
  canManageProjectMember,
  canManageProjectMembers,
  getAssignableProjectMembershipRoles,
} from './projectMembershipPermissions'

describe('project membership permissions', () => {
  it('allows owners and managers to manage project members', () => {
    expect(canManageProjectMembers('OWNER')).toBe(true)
    expect(canManageProjectMembers('MANAGER')).toBe(true)
  })

  it('keeps contributors and viewers read-only', () => {
    expect(canManageProjectMembers('CONTRIBUTOR')).toBe(false)
    expect(canManageProjectMembers('VIEWER')).toBe(false)
    expect(canManageProjectMembers(null)).toBe(false)
  })

  it('allows owners to assign all managed roles', () => {
    expect(
      getAssignableProjectMembershipRoles('OWNER'),
    ).toEqual([
      'MANAGER',
      'CONTRIBUTOR',
      'VIEWER',
    ])
  })

  it('limits managers to contributor and viewer roles', () => {
    expect(
      getAssignableProjectMembershipRoles('MANAGER'),
    ).toEqual([
      'CONTRIBUTOR',
      'VIEWER',
    ])
  })

  it('does not expose assignable roles to read-only members', () => {
    expect(
      getAssignableProjectMembershipRoles('CONTRIBUTOR'),
    ).toEqual([])

    expect(
      getAssignableProjectMembershipRoles('VIEWER'),
    ).toEqual([])
  })

  it('never allows the owner membership to be managed generically', () => {
    expect(
      canManageProjectMember('OWNER', 'OWNER'),
    ).toBe(false)

    expect(
      canManageProjectMember('MANAGER', 'OWNER'),
    ).toBe(false)
  })

  it('allows owners to manage non-owner memberships', () => {
    expect(
      canManageProjectMember('OWNER', 'MANAGER'),
    ).toBe(true)

    expect(
      canManageProjectMember('OWNER', 'CONTRIBUTOR'),
    ).toBe(true)

    expect(
      canManageProjectMember('OWNER', 'VIEWER'),
    ).toBe(true)
  })

  it('prevents managers from managing other managers', () => {
    expect(
      canManageProjectMember('MANAGER', 'MANAGER'),
    ).toBe(false)
  })

  it('allows managers to manage contributors and viewers', () => {
    expect(
      canManageProjectMember('MANAGER', 'CONTRIBUTOR'),
    ).toBe(true)

    expect(
      canManageProjectMember('MANAGER', 'VIEWER'),
    ).toBe(true)
  })
})
